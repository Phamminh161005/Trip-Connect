"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getMyAgentProfile } from "@/lib/api/agent";
import type { AgentProfileResponse } from "@/types/agent";

export const AGENT_PROFILE_KEY = ["agent-profile"] as const;

export function useAgentProfile() {
  return useQuery({ queryKey: AGENT_PROFILE_KEY, queryFn: getMyAgentProfile });
}

/** Cập nhật dữ liệu hồ sơ đang hiển thị từ kết quả API trả về (không cần tải lại). */
export function useSetAgentProfile() {
  const queryClient = useQueryClient();
  return {
    set: (profile: AgentProfileResponse) => queryClient.setQueryData(AGENT_PROFILE_KEY, profile),
    refetch: () => queryClient.invalidateQueries({ queryKey: AGENT_PROFILE_KEY }),
  };
}
