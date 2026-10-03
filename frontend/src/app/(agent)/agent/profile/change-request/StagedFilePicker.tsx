"use client";

import { useRef } from "react";
import { FileText, Upload, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ACCEPTED_DOCUMENT_TYPES, checkDocumentFile } from "@/lib/agent/documents";
import { formatFileSize } from "@/lib/format";

/**
 * Chọn file nhưng CHƯA tải lên — file được gửi cùng lúc với cả yêu cầu khi bấm "Gửi yêu cầu"
 * (Backend nhận chữ + file trong 1 request multipart).
 */
export function StagedFilePicker({
  label,
  currentFilename,
  file,
  onChange,
}: {
  label: string;
  currentFilename?: string | null;
  file: File | null;
  onChange: (file: File | null) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  const pick = (picked: File | undefined) => {
    if (!picked) return;
    const problem = checkDocumentFile(picked);
    if (problem) toast.error(problem);
    else onChange(picked);
    if (inputRef.current) inputRef.current.value = "";
  };

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border p-4">
      <div className="min-w-0">
        <p className="font-medium">{label}</p>
        {file ? (
          <p className="flex items-center gap-1.5 text-sm font-medium text-primary">
            <FileText className="size-3.5 shrink-0" />
            <span className="truncate">Thay bằng: {file.name}</span>
            <span className="text-muted-foreground">· {formatFileSize(file.size)}</span>
          </p>
        ) : (
          <p className="truncate text-sm text-muted-foreground">Hiện tại: {currentFilename ?? "chưa có"}</p>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_DOCUMENT_TYPES.join(",")}
        className="hidden"
        onChange={(e) => pick(e.target.files?.[0])}
      />
      {file ? (
        <Button type="button" variant="ghost" size="sm" className="rounded-lg" onClick={() => onChange(null)}>
          <X /> Bỏ chọn
        </Button>
      ) : (
        <Button type="button" variant="outline" size="sm" className="rounded-lg" onClick={() => inputRef.current?.click()}>
          <Upload /> Chọn file thay thế
        </Button>
      )}
    </div>
  );
}
