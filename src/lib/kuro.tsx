import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";

/** Hook returning the current user's Kuro Points balance + streak. */
export function useKuro() {
  const { user } = useAuth();
  const qc = useQueryClient();

  const q = useQuery({
    enabled: !!user,
    queryKey: ["kuro", user?.id],
    queryFn: async () => {
      const { data } = await supabase
        .from("profiles")
        .select("credits,streak,last_login_at")
        .eq("id", user!.id)
        .maybeSingle();
      return data ?? { credits: 0, streak: 0, last_login_at: null };
    },
    staleTime: 15_000,
  });

  return {
    credits: q.data?.credits ?? 0,
    streak: q.data?.streak ?? 0,
    lastLogin: q.data?.last_login_at ?? null,
    refresh: () => qc.invalidateQueries({ queryKey: ["kuro", user?.id] }),
  };
}

/** Auto-claim once per UTC day on app boot. */
export function useDailyLoginClaim() {
  const { user } = useAuth();
  const qc = useQueryClient();
  useEffect(() => {
    if (!user) return;
    const key = `kuro:lastClaim:${user.id}`;
    const today = new Date().toISOString().slice(0, 10);
    if (localStorage.getItem(key) === today) return;
    (async () => {
      const { data, error } = await supabase.rpc("claim_daily_login");
      if (error) return;
      localStorage.setItem(key, today);
      const r = data as { claimed?: boolean; reward?: number; streak?: number } | null;
      if (r?.claimed) {
        toast.success(`+${r.reward} Kuro Points`, {
          description: `Day ${r.streak} streak — keep it up!`,
        });
        qc.invalidateQueries({ queryKey: ["kuro", user.id] });
      }
    })();
  }, [user, qc]);
}

/** Increment a user mission for today. Inserts row on first call. */
export async function bumpMission(userId: string, code: string, by = 1) {
  const { data: m } = await supabase.from("daily_missions").select("id,target,reward").eq("code", code).maybeSingle();
  if (!m) return;
  const today = new Date().toISOString().slice(0, 10);
  const { data: existing } = await supabase
    .from("user_missions")
    .select("id,progress,completed,claimed")
    .eq("user_id", userId)
    .eq("mission_id", m.id)
    .eq("day", today)
    .maybeSingle();

  if (!existing) {
    const progress = Math.min(by, m.target);
    await supabase.from("user_missions").insert({
      user_id: userId,
      mission_id: m.id,
      progress,
      completed: progress >= m.target,
      day: today,
    });
    return;
  }
  if (existing.completed) return;
  const progress = Math.min(existing.progress + by, m.target);
  await supabase
    .from("user_missions")
    .update({ progress, completed: progress >= m.target, updated_at: new Date().toISOString() })
    .eq("id", existing.id);
}
