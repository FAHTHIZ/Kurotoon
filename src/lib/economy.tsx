import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { TRUST_LEVELS, type TrustLevelId } from "@/lib/brand";
import { toast } from "sonner";

export type HubProfile = {
  username: string | null;
  avatar_url: string | null;
  bio: string | null;
  credits: number;
  streak: number;
  trust_level: TrustLevelId;
  creator_name: string | null;
  is_creator: boolean;
  contribution_xp: number;
};

/** The multi-role hub profile (reader + contributor + creator in one row). */
export function useHubProfile() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const q = useQuery({
    enabled: !!user,
    queryKey: ["hub-profile", user?.id],
    queryFn: async (): Promise<HubProfile | null> => {
      const { data } = await supabase
        .from("profiles")
        .select("username,avatar_url,bio,credits,streak,trust_level,creator_name,is_creator,contribution_xp")
        .eq("id", user!.id)
        .maybeSingle();
      return (data as HubProfile | null) ?? null;
    },
    staleTime: 20_000,
  });
  return { ...q, refresh: () => qc.invalidateQueries({ queryKey: ["hub-profile", user?.id] }) };
}

export function trustLabel(id: string | null | undefined) {
  return TRUST_LEVELS.find((t) => t.id === id) ?? TRUST_LEVELS[0];
}

export type ShopItem = {
  id: string;
  code: string;
  name: string;
  description: string;
  kind: string;
  price: number;
  rarity: string;
};

export function useShop() {
  const { user } = useAuth();
  const items = useQuery({
    queryKey: ["shop-items"],
    queryFn: async (): Promise<ShopItem[]> => {
      const { data } = await supabase
        .from("shop_items")
        .select("id,code,name,description,kind,price,rarity")
        .order("price");
      return (data as ShopItem[]) ?? [];
    },
    staleTime: 5 * 60_000,
  });

  const owned = useQuery({
    enabled: !!user,
    queryKey: ["owned-items", user?.id],
    queryFn: async (): Promise<string[]> => {
      const { data } = await supabase.from("user_items").select("item_id").eq("user_id", user!.id);
      return (data ?? []).map((r) => r.item_id as string);
    },
  });

  return { items, owned };
}

export function usePurchase(onDone?: () => void) {
  const qc = useQueryClient();
  const { user } = useAuth();
  return useMutation({
    mutationFn: async (code: string) => {
      const { data, error } = await supabase.rpc("purchase_shop_item", { _code: code });
      if (error) throw new Error(error.message);
      const r = data as { ok?: boolean; error?: string } | null;
      if (!r?.ok) throw new Error(r?.error ?? "purchase failed");
      return r;
    },
    onSuccess: () => {
      toast.success("Unlocked!", { description: "Added to your collection." });
      qc.invalidateQueries({ queryKey: ["owned-items", user?.id] });
      qc.invalidateQueries({ queryKey: ["hub-profile", user?.id] });
      qc.invalidateQueries({ queryKey: ["kuro", user?.id] });
      onDone?.();
    },
    onError: (e: Error) => toast.error(e.message),
  });
}
