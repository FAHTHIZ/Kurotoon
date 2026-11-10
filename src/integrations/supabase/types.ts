export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.5"
  }
  public: {
    Tables: {
      achievements: {
        Row: {
          code: string
          description: string | null
          icon: string | null
          id: string
          reward: number
          title: string
        }
        Insert: {
          code: string
          description?: string | null
          icon?: string | null
          id?: string
          reward?: number
          title: string
        }
        Update: {
          code?: string
          description?: string | null
          icon?: string | null
          id?: string
          reward?: number
          title?: string
        }
        Relationships: []
      }
      bookmarks: {
        Row: {
          created_at: string
          id: string
          series_id: string
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          series_id: string
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          series_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "bookmarks_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "bookmarks_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series_ready"
            referencedColumns: ["id"]
          },
        ]
      }
      chapter_comments: {
        Row: {
          body: string | null
          chapter_id: string
          created_at: string
          id: string
          sticker_id: string | null
          user_id: string
        }
        Insert: {
          body?: string | null
          chapter_id: string
          created_at?: string
          id?: string
          sticker_id?: string | null
          user_id: string
        }
        Update: {
          body?: string | null
          chapter_id?: string
          created_at?: string
          id?: string
          sticker_id?: string | null
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "chapter_comments_chapter_id_fkey"
            columns: ["chapter_id"]
            isOneToOne: false
            referencedRelation: "chapters"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "chapter_comments_sticker_id_fkey"
            columns: ["sticker_id"]
            isOneToOne: false
            referencedRelation: "stickers"
            referencedColumns: ["id"]
          },
        ]
      }
      chapter_integrity_issues: {
        Row: {
          actual_pages: number | null
          chapter_id: string
          checked_at: string
          expected_pages: number | null
          extra_numbers: number[] | null
          id: string
          issue: string
          last_repair_at: string | null
          missing_numbers: number[] | null
          next_repair_after: string | null
          repair_attempts: number
          resolved_at: string | null
          series_id: string
        }
        Insert: {
          actual_pages?: number | null
          chapter_id: string
          checked_at?: string
          expected_pages?: number | null
          extra_numbers?: number[] | null
          id?: string
          issue: string
          last_repair_at?: string | null
          missing_numbers?: number[] | null
          next_repair_after?: string | null
          repair_attempts?: number
          resolved_at?: string | null
          series_id: string
        }
        Update: {
          actual_pages?: number | null
          chapter_id?: string
          checked_at?: string
          expected_pages?: number | null
          extra_numbers?: number[] | null
          id?: string
          issue?: string
          last_repair_at?: string | null
          missing_numbers?: number[] | null
          next_repair_after?: string | null
          repair_attempts?: number
          resolved_at?: string | null
          series_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "chapter_integrity_issues_chapter_id_fkey"
            columns: ["chapter_id"]
            isOneToOne: false
            referencedRelation: "chapters"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "chapter_integrity_issues_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "chapter_integrity_issues_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series_ready"
            referencedColumns: ["id"]
          },
        ]
      }
      chapter_unlocks: {
        Row: {
          chapter_id: string
          cost: number
          id: string
          unlocked_at: string
          user_id: string
        }
        Insert: {
          chapter_id: string
          cost?: number
          id?: string
          unlocked_at?: string
          user_id: string
        }
        Update: {
          chapter_id?: string
          cost?: number
          id?: string
          unlocked_at?: string
          user_id?: string
        }
        Relationships: []
      }
      chapters: {
        Row: {
          created_at: string
          external_id: string | null
          id: string
          last_403_at: string | null
          number: number
          page_count: number
          pages_refreshed_at: string | null
          series_id: string
          title: string | null
        }
        Insert: {
          created_at?: string
          external_id?: string | null
          id?: string
          last_403_at?: string | null
          number: number
          page_count?: number
          pages_refreshed_at?: string | null
          series_id: string
          title?: string | null
        }
        Update: {
          created_at?: string
          external_id?: string | null
          id?: string
          last_403_at?: string | null
          number?: number
          page_count?: number
          pages_refreshed_at?: string | null
          series_id?: string
          title?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "chapters_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "chapters_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series_ready"
            referencedColumns: ["id"]
          },
        ]
      }
      comment_reactions: {
        Row: {
          comment_id: string
          created_at: string
          id: string
          sticker_id: string
          user_id: string
        }
        Insert: {
          comment_id: string
          created_at?: string
          id?: string
          sticker_id: string
          user_id: string
        }
        Update: {
          comment_id?: string
          created_at?: string
          id?: string
          sticker_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "comment_reactions_comment_id_fkey"
            columns: ["comment_id"]
            isOneToOne: false
            referencedRelation: "chapter_comments"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "comment_reactions_sticker_id_fkey"
            columns: ["sticker_id"]
            isOneToOne: false
            referencedRelation: "stickers"
            referencedColumns: ["id"]
          },
        ]
      }
      contributions: {
        Row: {
          created_at: string
          id: string
          kind: string
          note: string | null
          payload: Json
          reviewed_at: string | null
          reviewed_by: string | null
          status: string
          target_ref: string
          target_title: string | null
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          kind: string
          note?: string | null
          payload?: Json
          reviewed_at?: string | null
          reviewed_by?: string | null
          status?: string
          target_ref: string
          target_title?: string | null
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          kind?: string
          note?: string | null
          payload?: Json
          reviewed_at?: string | null
          reviewed_by?: string | null
          status?: string
          target_ref?: string
          target_title?: string | null
          user_id?: string
        }
        Relationships: []
      }
      creator_chapters: {
        Row: {
          created_at: string
          id: string
          number: number
          owner_id: string
          pages: Json
          published: boolean
          title: string | null
          work_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          number: number
          owner_id: string
          pages?: Json
          published?: boolean
          title?: string | null
          work_id: string
        }
        Update: {
          created_at?: string
          id?: string
          number?: number
          owner_id?: string
          pages?: Json
          published?: boolean
          title?: string | null
          work_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "creator_chapters_work_id_fkey"
            columns: ["work_id"]
            isOneToOne: false
            referencedRelation: "creator_works"
            referencedColumns: ["id"]
          },
        ]
      }
      creator_follows: {
        Row: {
          created_at: string
          creator_id: string
          follower_id: string
          id: string
        }
        Insert: {
          created_at?: string
          creator_id: string
          follower_id: string
          id?: string
        }
        Update: {
          created_at?: string
          creator_id?: string
          follower_id?: string
          id?: string
        }
        Relationships: []
      }
      creator_works: {
        Row: {
          atmosphere: string | null
          cover_url: string | null
          created_at: string
          id: string
          owner_id: string
          slug: string
          status: string
          synopsis: string
          tags: string[]
          title: string
          updated_at: string
          visibility: string
        }
        Insert: {
          atmosphere?: string | null
          cover_url?: string | null
          created_at?: string
          id?: string
          owner_id: string
          slug: string
          status?: string
          synopsis?: string
          tags?: string[]
          title: string
          updated_at?: string
          visibility?: string
        }
        Update: {
          atmosphere?: string | null
          cover_url?: string | null
          created_at?: string
          id?: string
          owner_id?: string
          slug?: string
          status?: string
          synopsis?: string
          tags?: string[]
          title?: string
          updated_at?: string
          visibility?: string
        }
        Relationships: []
      }
      daily_missions: {
        Row: {
          code: string
          description: string | null
          id: string
          reward: number
          target: number
          title: string
        }
        Insert: {
          code: string
          description?: string | null
          id?: string
          reward?: number
          target?: number
          title: string
        }
        Update: {
          code?: string
          description?: string | null
          id?: string
          reward?: number
          target?: number
          title?: string
        }
        Relationships: []
      }
      pages: {
        Row: {
          chapter_id: string
          id: string
          image_url: string
          page_number: number
        }
        Insert: {
          chapter_id: string
          id?: string
          image_url: string
          page_number: number
        }
        Update: {
          chapter_id?: string
          id?: string
          image_url?: string
          page_number?: number
        }
        Relationships: [
          {
            foreignKeyName: "pages_chapter_id_fkey"
            columns: ["chapter_id"]
            isOneToOne: false
            referencedRelation: "chapters"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          active_badge: string | null
          avatar_url: string | null
          banner_url: string | null
          bio: string | null
          contribution_xp: number
          created_at: string
          creator_name: string | null
          credits: number
          id: string
          is_creator: boolean
          last_login_at: string | null
          streak: number
          trust_level: string
          username: string
        }
        Insert: {
          active_badge?: string | null
          avatar_url?: string | null
          banner_url?: string | null
          bio?: string | null
          contribution_xp?: number
          created_at?: string
          creator_name?: string | null
          credits?: number
          id: string
          is_creator?: boolean
          last_login_at?: string | null
          streak?: number
          trust_level?: string
          username: string
        }
        Update: {
          active_badge?: string | null
          avatar_url?: string | null
          banner_url?: string | null
          bio?: string | null
          contribution_xp?: number
          created_at?: string
          creator_name?: string | null
          credits?: number
          id?: string
          is_creator?: boolean
          last_login_at?: string | null
          streak?: number
          trust_level?: string
          username?: string
        }
        Relationships: []
      }
      reading_progress: {
        Row: {
          chapter_id: string
          id: string
          page_number: number
          series_id: string
          updated_at: string
          user_id: string
        }
        Insert: {
          chapter_id: string
          id?: string
          page_number?: number
          series_id: string
          updated_at?: string
          user_id: string
        }
        Update: {
          chapter_id?: string
          id?: string
          page_number?: number
          series_id?: string
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "reading_progress_chapter_id_fkey"
            columns: ["chapter_id"]
            isOneToOne: false
            referencedRelation: "chapters"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "reading_progress_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "reading_progress_series_id_fkey"
            columns: ["series_id"]
            isOneToOne: false
            referencedRelation: "series_ready"
            referencedColumns: ["id"]
          },
        ]
      }
      reel_likes: {
        Row: {
          created_at: string
          id: string
          reel_id: string
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          reel_id: string
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          reel_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "reel_likes_reel_id_fkey"
            columns: ["reel_id"]
            isOneToOne: false
            referencedRelation: "reels"
            referencedColumns: ["id"]
          },
        ]
      }
      reels: {
        Row: {
          atmosphere: string | null
          caption: string
          chapter_ref: string | null
          created_at: string
          created_by: string | null
          id: string
          image_url: string
          likes: number
          series_ref: string
          series_title: string
        }
        Insert: {
          atmosphere?: string | null
          caption?: string
          chapter_ref?: string | null
          created_at?: string
          created_by?: string | null
          id?: string
          image_url: string
          likes?: number
          series_ref: string
          series_title: string
        }
        Update: {
          atmosphere?: string | null
          caption?: string
          chapter_ref?: string | null
          created_at?: string
          created_by?: string | null
          id?: string
          image_url?: string
          likes?: number
          series_ref?: string
          series_title?: string
        }
        Relationships: []
      }
      repo_items: {
        Row: {
          collection_name: string
          cover_url: string | null
          created_at: string
          id: string
          position: number
          repo_id: string
          series_ref: string
          title: string
          user_id: string
        }
        Insert: {
          collection_name?: string
          cover_url?: string | null
          created_at?: string
          id?: string
          position?: number
          repo_id: string
          series_ref: string
          title: string
          user_id: string
        }
        Update: {
          collection_name?: string
          cover_url?: string | null
          created_at?: string
          id?: string
          position?: number
          repo_id?: string
          series_ref?: string
          title?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "repo_items_repo_id_fkey"
            columns: ["repo_id"]
            isOneToOne: false
            referencedRelation: "repositories"
            referencedColumns: ["id"]
          },
        ]
      }
      repo_sources: {
        Row: {
          base_url: string | null
          code: string
          created_at: string
          enabled: boolean
          id: string
          lang: string
          name: string
          nsfw: boolean
          repo_id: string
          spec: Json
          updated_at: string
          user_id: string
          version: string | null
        }
        Insert: {
          base_url?: string | null
          code: string
          created_at?: string
          enabled?: boolean
          id?: string
          lang?: string
          name: string
          nsfw?: boolean
          repo_id: string
          spec?: Json
          updated_at?: string
          user_id: string
          version?: string | null
        }
        Update: {
          base_url?: string | null
          code?: string
          created_at?: string
          enabled?: boolean
          id?: string
          lang?: string
          name?: string
          nsfw?: boolean
          repo_id?: string
          spec?: Json
          updated_at?: string
          user_id?: string
          version?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "repo_sources_repo_id_fkey"
            columns: ["repo_id"]
            isOneToOne: false
            referencedRelation: "repositories"
            referencedColumns: ["id"]
          },
        ]
      }
      repositories: {
        Row: {
          builtin: boolean
          created_at: string
          description: string | null
          enabled: boolean
          icon_url: string | null
          id: string
          kind: string
          last_synced_at: string | null
          name: string
          updated_at: string
          url: string | null
          user_id: string
        }
        Insert: {
          builtin?: boolean
          created_at?: string
          description?: string | null
          enabled?: boolean
          icon_url?: string | null
          id?: string
          kind?: string
          last_synced_at?: string | null
          name: string
          updated_at?: string
          url?: string | null
          user_id: string
        }
        Update: {
          builtin?: boolean
          created_at?: string
          description?: string | null
          enabled?: boolean
          icon_url?: string | null
          id?: string
          kind?: string
          last_synced_at?: string | null
          name?: string
          updated_at?: string
          url?: string | null
          user_id?: string
        }
        Relationships: []
      }
      series: {
        Row: {
          artist: string | null
          author: string | null
          cover_url: string | null
          created_at: string
          description: string | null
          external_id: string | null
          genres: string[]
          id: string
          rating: number | null
          slug: string
          source_id: string | null
          status: string
          title: string
          updated_at: string
          views: number
        }
        Insert: {
          artist?: string | null
          author?: string | null
          cover_url?: string | null
          created_at?: string
          description?: string | null
          external_id?: string | null
          genres?: string[]
          id?: string
          rating?: number | null
          slug: string
          source_id?: string | null
          status?: string
          title: string
          updated_at?: string
          views?: number
        }
        Update: {
          artist?: string | null
          author?: string | null
          cover_url?: string | null
          created_at?: string
          description?: string | null
          external_id?: string | null
          genres?: string[]
          id?: string
          rating?: number | null
          slug?: string
          source_id?: string | null
          status?: string
          title?: string
          updated_at?: string
          views?: number
        }
        Relationships: [
          {
            foreignKeyName: "series_source_id_fkey"
            columns: ["source_id"]
            isOneToOne: false
            referencedRelation: "sync_sources"
            referencedColumns: ["id"]
          },
        ]
      }
      shop_items: {
        Row: {
          code: string
          created_at: string
          description: string
          id: string
          kind: string
          name: string
          payload: Json
          price: number
          rarity: string
        }
        Insert: {
          code: string
          created_at?: string
          description?: string
          id?: string
          kind: string
          name: string
          payload?: Json
          price?: number
          rarity?: string
        }
        Update: {
          code?: string
          created_at?: string
          description?: string
          id?: string
          kind?: string
          name?: string
          payload?: Json
          price?: number
          rarity?: string
        }
        Relationships: []
      }
      stickers: {
        Row: {
          created_at: string
          emoji: string
          id: string
          image_url: string | null
          name: string
          price: number
          rarity: string
        }
        Insert: {
          created_at?: string
          emoji: string
          id?: string
          image_url?: string | null
          name: string
          price?: number
          rarity?: string
        }
        Update: {
          created_at?: string
          emoji?: string
          id?: string
          image_url?: string | null
          name?: string
          price?: number
          rarity?: string
        }
        Relationships: []
      }
      sync_jobs: {
        Row: {
          attempts: number
          created_at: string
          external_id: string
          id: string
          kind: string
          last_error: string | null
          locked_at: string | null
          max_attempts: number
          payload: Json
          run_after: string
          source_id: string
          status: string
          updated_at: string
        }
        Insert: {
          attempts?: number
          created_at?: string
          external_id: string
          id?: string
          kind: string
          last_error?: string | null
          locked_at?: string | null
          max_attempts?: number
          payload?: Json
          run_after?: string
          source_id: string
          status?: string
          updated_at?: string
        }
        Update: {
          attempts?: number
          created_at?: string
          external_id?: string
          id?: string
          kind?: string
          last_error?: string | null
          locked_at?: string | null
          max_attempts?: number
          payload?: Json
          run_after?: string
          source_id?: string
          status?: string
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "sync_jobs_source_id_fkey"
            columns: ["source_id"]
            isOneToOne: false
            referencedRelation: "sync_sources"
            referencedColumns: ["id"]
          },
        ]
      }
      sync_sources: {
        Row: {
          base_url: string
          code: string
          confidence_score: number
          config: Json
          created_at: string
          id: string
          kind: string
          last_crawled_at: string | null
          last_error: string | null
          name: string
          rate_limit_per_min: number
          status: string
          updated_at: string
        }
        Insert: {
          base_url: string
          code: string
          confidence_score?: number
          config?: Json
          created_at?: string
          id?: string
          kind?: string
          last_crawled_at?: string | null
          last_error?: string | null
          name: string
          rate_limit_per_min?: number
          status?: string
          updated_at?: string
        }
        Update: {
          base_url?: string
          code?: string
          confidence_score?: number
          config?: Json
          created_at?: string
          id?: string
          kind?: string
          last_crawled_at?: string | null
          last_error?: string | null
          name?: string
          rate_limit_per_min?: number
          status?: string
          updated_at?: string
        }
        Relationships: []
      }
      tips: {
        Row: {
          amount: number
          created_at: string
          from_user: string
          id: string
          message: string | null
          series_id: string | null
          to_user: string
        }
        Insert: {
          amount: number
          created_at?: string
          from_user: string
          id?: string
          message?: string | null
          series_id?: string | null
          to_user: string
        }
        Update: {
          amount?: number
          created_at?: string
          from_user?: string
          id?: string
          message?: string | null
          series_id?: string | null
          to_user?: string
        }
        Relationships: []
      }
      transactions: {
        Row: {
          amount: number
          created_at: string
          id: string
          meta: Json | null
          reason: string
          user_id: string
        }
        Insert: {
          amount: number
          created_at?: string
          id?: string
          meta?: Json | null
          reason: string
          user_id: string
        }
        Update: {
          amount?: number
          created_at?: string
          id?: string
          meta?: Json | null
          reason?: string
          user_id?: string
        }
        Relationships: []
      }
      user_achievements: {
        Row: {
          achievement_id: string
          id: string
          unlocked_at: string
          user_id: string
        }
        Insert: {
          achievement_id: string
          id?: string
          unlocked_at?: string
          user_id: string
        }
        Update: {
          achievement_id?: string
          id?: string
          unlocked_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "user_achievements_achievement_id_fkey"
            columns: ["achievement_id"]
            isOneToOne: false
            referencedRelation: "achievements"
            referencedColumns: ["id"]
          },
        ]
      }
      user_items: {
        Row: {
          acquired_at: string
          id: string
          item_id: string
          user_id: string
        }
        Insert: {
          acquired_at?: string
          id?: string
          item_id: string
          user_id: string
        }
        Update: {
          acquired_at?: string
          id?: string
          item_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "user_items_item_id_fkey"
            columns: ["item_id"]
            isOneToOne: false
            referencedRelation: "shop_items"
            referencedColumns: ["id"]
          },
        ]
      }
      user_missions: {
        Row: {
          claimed: boolean
          completed: boolean
          day: string
          id: string
          mission_id: string
          progress: number
          updated_at: string
          user_id: string
        }
        Insert: {
          claimed?: boolean
          completed?: boolean
          day?: string
          id?: string
          mission_id: string
          progress?: number
          updated_at?: string
          user_id: string
        }
        Update: {
          claimed?: boolean
          completed?: boolean
          day?: string
          id?: string
          mission_id?: string
          progress?: number
          updated_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "user_missions_mission_id_fkey"
            columns: ["mission_id"]
            isOneToOne: false
            referencedRelation: "daily_missions"
            referencedColumns: ["id"]
          },
        ]
      }
      user_roles: {
        Row: {
          created_at: string
          id: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Update: {
          created_at?: string
          id?: string
          role?: Database["public"]["Enums"]["app_role"]
          user_id?: string
        }
        Relationships: []
      }
      user_stickers: {
        Row: {
          acquired_at: string
          id: string
          sticker_id: string
          user_id: string
        }
        Insert: {
          acquired_at?: string
          id?: string
          sticker_id: string
          user_id: string
        }
        Update: {
          acquired_at?: string
          id?: string
          sticker_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "user_stickers_sticker_id_fkey"
            columns: ["sticker_id"]
            isOneToOne: false
            referencedRelation: "stickers"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      series_ready: {
        Row: {
          artist: string | null
          author: string | null
          cover_url: string | null
          created_at: string | null
          description: string | null
          external_id: string | null
          genres: string[] | null
          id: string | null
          rating: number | null
          slug: string | null
          source_id: string | null
          status: string | null
          title: string | null
          updated_at: string | null
          views: number | null
        }
        Insert: {
          artist?: string | null
          author?: string | null
          cover_url?: string | null
          created_at?: string | null
          description?: string | null
          external_id?: string | null
          genres?: string[] | null
          id?: string | null
          rating?: number | null
          slug?: string | null
          source_id?: string | null
          status?: string | null
          title?: string | null
          updated_at?: string | null
          views?: number | null
        }
        Update: {
          artist?: string | null
          author?: string | null
          cover_url?: string | null
          created_at?: string | null
          description?: string | null
          external_id?: string | null
          genres?: string[] | null
          id?: string | null
          rating?: number | null
          slug?: string | null
          source_id?: string | null
          status?: string | null
          title?: string | null
          updated_at?: string | null
          views?: number | null
        }
        Relationships: [
          {
            foreignKeyName: "series_source_id_fkey"
            columns: ["source_id"]
            isOneToOne: false
            referencedRelation: "sync_sources"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Functions: {
      admin_enqueue_rescrape: {
        Args: { _chapter_id?: string; _series_id?: string }
        Returns: number
      }
      award_points: {
        Args: {
          _amount: number
          _meta?: Json
          _reason: string
          _user_id: string
        }
        Returns: number
      }
      become_creator: { Args: { _creator_name: string }; Returns: Json }
      claim_daily_login: { Args: never; Returns: Json }
      claim_sync_jobs: {
        Args: { _limit: number }
        Returns: {
          attempts: number
          created_at: string
          external_id: string
          id: string
          kind: string
          last_error: string | null
          locked_at: string | null
          max_attempts: number
          payload: Json
          run_after: string
          source_id: string
          status: string
          updated_at: string
        }[]
        SetofOptions: {
          from: "*"
          to: "sync_jobs"
          isOneToOne: false
          isSetofReturn: true
        }
      }
      has_role: {
        Args: {
          _role: Database["public"]["Enums"]["app_role"]
          _user_id: string
        }
        Returns: boolean
      }
      is_chapter_free: { Args: { _chapter_id: string }; Returns: boolean }
      list_genres: {
        Args: never
        Returns: {
          count: number
          genre: string
        }[]
      }
      purchase_shop_item: { Args: { _code: string }; Returns: Json }
      scan_chapter_integrity: {
        Args: { _limit?: number }
        Returns: {
          actual: number
          chapter_id: string
          distinct_actual: number
          expected: number
          external_id: string
          extra: number[]
          missing: number[]
          series_id: string
        }[]
      }
      submit_contribution: {
        Args: {
          _kind: string
          _note: string
          _payload: Json
          _target_ref: string
          _target_title: string
        }
        Returns: Json
      }
      toggle_reel_like: { Args: { _reel_id: string }; Returns: Json }
      transfer_points: {
        Args: {
          _amount: number
          _message: string
          _series_id: string
          _to_user: string
        }
        Returns: undefined
      }
      unlock_chapter: { Args: { _chapter_id: string }; Returns: Json }
    }
    Enums: {
      app_role: "admin" | "moderator" | "user"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      app_role: ["admin", "moderator", "user"],
    },
  },
} as const
