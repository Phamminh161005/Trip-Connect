"use client";

import { CircleAlert, Eye, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { REQUIRED_DOCUMENTS, openTemporaryDocument } from "@/lib/agent/documents";
import { getAgentDocumentUrlForAdmin } from "@/lib/api/admin";
import { formatDateTime, formatFileSize } from "@/lib/format";
import type { AgentDocumentResponse } from "@/types/agent";

/** Mở giấy tờ private bằng link tạm thời của API quản trị. */
function ViewButton({ profileId, document }: { profileId: number; document: AgentDocumentResponse }) {
  return (
    <Button
      variant="outline"
      size="sm"
      className="shrink-0 rounded-lg"
      onClick={() => openTemporaryDocument(() => getAgentDocumentUrlForAdmin(profileId, document.id))}
    >
      <Eye /> Xem
    </Button>
  );
}

export function DocumentItem({ profileId, document, label }: { profileId: number; document: AgentDocumentResponse; label?: string }) {
  return (
    <li className="flex items-center justify-between gap-3 rounded-xl border px-3 py-2.5">
      <div className="min-w-0">
        <p className="text-sm font-medium">{label ?? document.typeLabel}</p>
        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <FileText className="size-3.5 shrink-0" />
          <span className="truncate">{document.originalFilename ?? "Tệp đính kèm"}</span>
          <span className="shrink-0">
            · {formatFileSize(document.sizeBytes)} · {formatDateTime(document.uploadedAt)}
          </span>
        </p>
      </div>
      <ViewButton profileId={profileId} document={document} />
    </li>
  );
}

/** Giấy tờ đang hiệu lực của hồ sơ: 4 loại bắt buộc (báo thiếu nếu chưa có) + giấy tờ khác. */
export function AdminDocumentList({ profileId, documents }: { profileId: number; documents: AgentDocumentResponse[] }) {
  const active = documents.filter((d) => d.status === "ACTIVE");
  const others = active.filter((d) => d.type === "OTHER");

  return (
    <ul className="flex flex-col gap-2">
      {REQUIRED_DOCUMENTS.map(({ type, label }) => {
        const document = active.find((d) => d.type === type);
        return document ? (
          <DocumentItem key={type} profileId={profileId} document={document} label={label} />
        ) : (
          <li key={type} className="flex items-center gap-2 rounded-xl border border-dashed px-3 py-2.5 text-sm">
            <CircleAlert className="size-4 text-destructive" />
            <span className="font-medium">{label}</span>
            <span className="text-destructive">— chưa tải lên</span>
          </li>
        );
      })}
      {others.map((document) => (
        <DocumentItem key={document.id} profileId={profileId} document={document} label="Giấy tờ khác" />
      ))}
    </ul>
  );
}
