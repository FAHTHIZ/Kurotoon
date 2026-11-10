CREATE POLICY "Own transactions insert" ON public.transactions
  FOR INSERT WITH CHECK (auth.uid() = user_id);