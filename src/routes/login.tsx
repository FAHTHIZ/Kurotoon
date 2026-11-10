import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import { motion } from "framer-motion";

export const Route = createFileRoute("/login")({
  head: () => ({ meta: [{ title: "Sign in — KuroToon" }] }),
  component: Login,
});

function Login() {
  const { signIn, signUp } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<"in" | "up">("in");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [username, setUsername] = useState("");
  const [busy, setBusy] = useState(false);

  const friendly = (msg: string) => {
    const m = msg.toLowerCase();
    if (m.includes("weak") || m.includes("pwned"))
      return "That password appears in known breaches — pick a longer, more unique one.";
    if (m.includes("invalid login")) return "Wrong email or password.";
    if (m.includes("already registered")) return "That email already has an account — sign in instead.";
    if (m.includes("email not confirmed")) return "Confirm your email first — check your inbox.";
    if (m.includes("fetch") || m.includes("network") || m.includes("521"))
      return "Can't reach the server right now. Try again in a moment.";
    return msg;
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 8) return toast.error("Use at least 8 characters.");
    setBusy(true);
    const { error } = mode === "in"
      ? await signIn(email, password)
      : await signUp(email, password, username || email.split("@")[0]);
    setBusy(false);
    if (error) return toast.error(friendly(error));
    toast.success(mode === "in" ? "Welcome back!" : "Account created!");
    navigate({ to: "/" });
  };


  return (
    <div className="mx-auto max-w-sm py-8">
      <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
        className="rounded-2xl bg-[color:var(--surface)] p-6 ring-1 ring-border">
        <div className="mb-5 text-center">
          <div className="mx-auto grid h-12 w-12 place-items-center rounded-xl bg-[image:var(--gradient-accent)] text-lg font-black text-primary-foreground shadow-[var(--shadow-glow)]">K</div>
          <h1 className="mt-3 text-xl font-bold">{mode === "in" ? "Welcome back" : "Join KuroToon"}</h1>
          <p className="text-xs text-muted-foreground">{mode === "in" ? "Sign in to sync your library" : "Create an account to bookmark and sync"}</p>
        </div>

        <form onSubmit={submit} className="space-y-3">
          {mode === "up" && (
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username"
              className="w-full rounded-lg bg-background px-3 py-2.5 text-sm outline-none ring-1 ring-border focus:ring-primary" />
          )}
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email"
            className="w-full rounded-lg bg-background px-3 py-2.5 text-sm outline-none ring-1 ring-border focus:ring-primary" />
          <input type="password" required minLength={6} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password"
            className="w-full rounded-lg bg-background px-3 py-2.5 text-sm outline-none ring-1 ring-border focus:ring-primary" />
          <button disabled={busy} type="submit"
            className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60">
            {busy ? "…" : mode === "in" ? "Sign in" : "Create account"}
          </button>
        </form>

        <button onClick={() => setMode(m => m === "in" ? "up" : "in")}
          className="mt-4 w-full text-center text-xs text-muted-foreground hover:text-primary">
          {mode === "in" ? "New here? Create an account" : "Already have an account? Sign in"}
        </button>
        <Link to="/" className="mt-2 block text-center text-xs text-muted-foreground hover:text-foreground">← Back to home</Link>
      </motion.div>
    </div>
  );
}
