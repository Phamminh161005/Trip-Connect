"use client";

import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { AcceptingRequestsToggle } from "./AcceptingRequestsToggle";
import { BusinessInfoSection } from "./BusinessInfoSection";
import { ChangeRequestHistory } from "./ChangeRequestHistory";
import { DocumentsSection } from "./DocumentsSection";
import { ExpertiseSection } from "./ExpertiseSection";
import { StatusBanner } from "./StatusBanner";
import { SubmitSection } from "./SubmitSection";
import { useAgentProfile } from "./useAgentProfile";

export function AgentProfileView() {
  const { data: profile, isPending, isError, error, refetch } = useAgentProfile();

  if (isPending) {
    return (
      <div className="flex flex-col gap-6" aria-busy="true">
        <Skeleton className="h-24 rounded-2xl" />
        <Skeleton className="h-72 rounded-2xl" />
        <Skeleton className="h-96 rounded-2xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center gap-4 rounded-2xl border bg-card p-10 text-center">
        <p className="text-muted-foreground">{errorMessage(error)}</p>
        <Button variant="outline" className="rounded-xl" onClick={() => refetch()}>
          <RefreshCw /> Thử lại
        </Button>
      </div>
    );
  }

  // Chỉ sửa thông tin pháp lý khi hồ sơ Nháp hoặc Cần bổ sung (khớp AgentProfile.isEditable() ở Backend)
  const editable = profile.status === "DRAFT" || profile.status === "NEEDS_REVISION";

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Hồ sơ kinh doanh</h1>
        <p className="text-muted-foreground">Thông tin pháp lý để TripConnect xác minh bạn là đối tác lữ hành hợp lệ.</p>
      </div>

      <StatusBanner
        profile={profile}
        action={profile.status === "APPROVED" ? <AcceptingRequestsToggle profile={profile} /> : undefined}
      />
      {editable && <SubmitSection profile={profile} />}
      <BusinessInfoSection profile={profile} editable={editable} />
      <DocumentsSection profile={profile} editable={editable} />
      <ExpertiseSection profile={profile} />
      {profile.status === "APPROVED" && <ChangeRequestHistory />}
    </div>
  );
}
