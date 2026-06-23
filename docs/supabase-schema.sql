CREATE TABLE IF NOT EXISTS public.players (
    uuid            TEXT        PRIMARY KEY,
    username        TEXT        NOT NULL,
    last_seen       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    has_mod         BOOLEAN     NOT NULL DEFAULT TRUE,
    dungeon_level   INTEGER     NOT NULL DEFAULT 0,
    class_1_level   INTEGER     NOT NULL DEFAULT 0,
    class_2_level   INTEGER     NOT NULL DEFAULT 0,
    class_3_level   INTEGER     NOT NULL DEFAULT 0,
    class_4_level   INTEGER     NOT NULL DEFAULT 0,
    class_5_level   INTEGER     NOT NULL DEFAULT 0,
    class_6_level   INTEGER     NOT NULL DEFAULT 0,
    class_7_level   INTEGER     NOT NULL DEFAULT 0,
    class_8_level   INTEGER     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS players_uuid_idx ON public.players (uuid);
CREATE INDEX IF NOT EXISTS players_has_mod_idx ON public.players (has_mod) WHERE has_mod = TRUE;

ALTER TABLE public.players ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read"      ON public.players FOR SELECT USING (true);
CREATE POLICY "Players can register" ON public.players FOR INSERT WITH CHECK (true);
CREATE POLICY "Players can update"   ON public.players FOR UPDATE USING (true);