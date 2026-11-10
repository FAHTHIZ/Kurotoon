import { createServerFn } from "@tanstack/react-start";
import { generateText, Output } from "ai";
import { z } from "zod";

const RecommendInput = z.object({
  mood: z.string().min(1).max(60),
  liked: z.array(z.string().max(120)).max(12).default([]),
});

const RecommendSchema = z.object({
  picks: z
    .array(
      z.object({
        title: z.string(),
        reason: z.string(),
        vibe: z.string(),
      }),
    )
    .max(8),
});

export type AiPick = { title: string; reason: string; vibe: string };

/** Kuro AI curator: turns a mood + reading history into search-ready picks. */
export const recommendSeries = createServerFn({ method: "POST" })
  .inputValidator((input: unknown) => RecommendInput.parse(input))
  .handler(async ({ data }): Promise<{ picks: AiPick[] }> => {
    const key = process.env["LOVABLE_API_KEY"];
    if (!key) return { picks: [] };

    const { createLovableAiGatewayProvider } = await import("./ai-gateway.server");
    const gateway = createLovableAiGatewayProvider(key);

    try {
      const result = await generateText({
        model: gateway("google/gemini-3.6-flash"),
        output: Output.object({ schema: RecommendSchema }),
        system:
          "You are Kuro, KuroToon's manga curator. Recommend widely-known manga, manhwa or webcomic titles that exist on MangaDex. " +
          "Return 6 picks. Use the exact official romanised title so it can be searched. Keep each reason under 18 words.",
        prompt: `Reader mood: ${data.mood}\nRecently enjoyed: ${
          data.liked.length ? data.liked.join(", ") : "unknown — pick strong entry points"
        }`,
      });
      const output = await result.output;
      return { picks: output.picks };
    } catch {
      return { picks: [] };
    }
  });

const SummarizeInput = z.object({
  title: z.string().max(200),
  description: z.string().max(4000),
});

/** Spoiler-free "why you might like this" blurb for a series page. */
export const seriesInsight = createServerFn({ method: "POST" })
  .inputValidator((input: unknown) => SummarizeInput.parse(input))
  .handler(async ({ data }): Promise<{ hook: string; tags: string[] }> => {
    const key = process.env["LOVABLE_API_KEY"];
    if (!key) return { hook: "", tags: [] };

    const { createLovableAiGatewayProvider } = await import("./ai-gateway.server");
    const gateway = createLovableAiGatewayProvider(key);

    try {
      const result = await generateText({
        model: gateway("google/gemini-3.6-flash"),
        output: Output.object({
          schema: z.object({ hook: z.string(), tags: z.array(z.string()).max(5) }),
        }),
        system:
          "You write spoiler-free reading hooks for a manga platform. One sentence, max 24 words, no spoilers, no plot twists. " +
          "Also return up to 5 short mood tags (one or two words each).",
        prompt: `Title: ${data.title}\nSynopsis: ${data.description.slice(0, 2000)}`,
      });
      const output = await result.output;
      return { hook: output.hook, tags: output.tags };
    } catch {
      return { hook: "", tags: [] };
    }
  });
