import { createOpenAICompatible } from "@ai-sdk/openai-compatible";

/** Shared provider for the KuroToon AI ecosystem. Server-only. */
export function createLovableAiGatewayProvider(apiKey: string) {
  return createOpenAICompatible({
    name: "lovable-ai-gateway",
    baseURL: "https://ai.gateway.lovable.dev/v1",
    headers: { "Lovable-API-Key": apiKey },
  });
}
