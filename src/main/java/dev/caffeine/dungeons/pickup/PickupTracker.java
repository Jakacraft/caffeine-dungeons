package dev.caffeine.dungeons.pickup;

import dev.caffeine.dungeons.util.RarityUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Detects "an item entered the inventory" by diffing total-count-per-item
 * snapshots on every client tick, instead of hooking a vanilla pickup
 * event. This is deliberate: on this server items are added via a give-
 * style command with no vanilla pickup animation/packet at all, so the
 * only signal that reliably covers give + craft + trade + an actual
 * pickup alike is "the inventory contains more of X than it did last tick."
 *
 * Summing counts across the WHOLE inventory (main + armor + offhand +
 * cursor stack) rather than diffing per slot means reordering the
 * hotbar, equipping armor, or drag-holding a stack on the cursor never
 * produces a false-positive notification, since none of those change the
 * total.
 *
 * Only items whose name or lore contains one of ALLOWED_TAGS actually
 * spawn a toast (see passesTypeFilter). This is both a deliberate scope
 * narrowing (materials/swords/bows only) and a practical workaround for
 * a GUI-click false-positive bug, since the junk that was triggering it
 * doesn't carry these tags.
 *
 * KNOWN LIMITATION: briefly parking a stack in a crafting-grid input slot
 * (a separate inventory from the player's own and from the cursor stack)
 * while a screen is open, then taking it back out, can still produce a
 * one-off duplicate toast for anything that does carry a matching tag.
 * Not handled in this version.
 */
public final class PickupTracker {

    public static final PickupTracker INSTANCE = new PickupTracker();

    private static final int MAX_VISIBLE = 9;

    private static final String TAG_MATERIAL = "ᴍᴀᴛᴇʀɪᴀʟ";
    private static final String TAG_SWORD = "ѕᴡᴏʀᴅ";
    private static final String TAG_BOW = "ʙᴏᴡ";

    private Map<String, Integer> lastCounts = null;
    private final List<PickupEntry> active = new CopyOnWriteArrayList<>();

    private PickupTracker() {}

    public void initializeBaseline(ClientPlayerEntity player) {
        lastCounts = snapshot(player, null);
    }

    public void clear() {
        lastCounts = null;
        active.clear();
    }

    public List<PickupEntry> getActive() {
        return active;
    }

    public void tick(ClientPlayerEntity player) {
        if (player == null) return;

        if (lastCounts == null) {
            initializeBaseline(player);
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, ItemStack> representative = new HashMap<>();
        Map<String, Integer> current = snapshot(player, representative);

        for (Map.Entry<String, Integer> e : current.entrySet()) {
            String key = e.getKey();
            int count = e.getValue();
            int before = lastCounts.getOrDefault(key, 0);
            int diff = count - before;
            if (diff > 0) {
                handleGain(key, representative.get(key), diff, now);
            }
        }

        lastCounts = current;
        active.removeIf(entry -> entry.isExpired(now));
    }

    private void handleGain(String key, ItemStack stack, int diff, long now) {
        if (stack == null || !passesTypeFilter(stack)) return;

        for (PickupEntry entry : active) {
            if (entry.groupKey.equals(key)) {
                entry.merge(diff, now);
                return;
            }
        }
        if (active.size() >= MAX_VISIBLE) {
            active.remove(0);
        }
        int color = RarityUtil.getRarityColor(stack); // TODO: match actual RarityUtil method name/signature
        active.add(new PickupEntry(key, stack.copyWithCount(1), color, diff, now));
    }

    private boolean passesTypeFilter(ItemStack stack) {
        if (containsTag(stack.getName().getString())) return true;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                if (containsTag(line.getString())) return true;
            }
        }
        return false;
    }

    private boolean containsTag(String s) {
        return s.contains(TAG_MATERIAL) || s.contains(TAG_SWORD) || s.contains(TAG_BOW);
    }

    private Map<String, Integer> snapshot(ClientPlayerEntity player, Map<String, ItemStack> representativeOut) {
        Map<String, Integer> counts = new HashMap<>();

        var inv = player.getInventory();
        int size = inv.size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getStack(i);
            addToPool(counts, representativeOut, stack);
        }

        ItemStack cursor = player.currentScreenHandler.getCursorStack();
        addToPool(counts, representativeOut, cursor);

        return counts;
    }

    private void addToPool(Map<String, Integer> counts, Map<String, ItemStack> representativeOut, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String key = groupKey(stack);
        counts.merge(key, stack.getCount(), Integer::sum);
        if (representativeOut != null) {
            representativeOut.putIfAbsent(key, stack);
        }
    }

    private String groupKey(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        String name = stack.getName().getString();
        return id + "|" + name;
    }
}