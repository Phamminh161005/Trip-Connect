"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { setAcceptingRequests } from "@/lib/api/agent";
import { errorMessage } from "@/lib/api/errors";
import type { AgentProfileResponse } from "@/types/agent";
import { useSetAgentProfile } from "./useAgentProfile";

/** Bật/tắt "Đang nhận yêu cầu tư vấn" — chỉ Agent đã được duyệt. */
export function AcceptingRequestsToggle({ profile }: { profile: AgentProfileResponse }) {
  const { set } = useSetAgentProfile();
  const [saving, setSaving] = useState(false);

  const toggle = async (accepting: boolean) => {
    setSaving(true);
    try {
      set(await setAcceptingRequests(accepting));
      toast.success(accepting ? "Đã bật nhận yêu cầu tư vấn" : "Đã tắt nhận yêu cầu tư vấn");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="flex shrink-0 items-center gap-3 rounded-xl bg-white/70 px-4 py-3">
      <Switch id="accepting-requests" checked={profile.acceptingRequests} disabled={saving} onCheckedChange={toggle} />
      <Label htmlFor="accepting-requests" className="cursor-pointer font-medium text-foreground">
        {profile.acceptingRequests ? "Đang nhận yêu cầu tư vấn" : "Tạm ngừng nhận yêu cầu"}
      </Label>
    </div>
  );
}
