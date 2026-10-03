"use client";

import { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { CircleCheck, CircleDashed, Eye, FileText, Plus, Trash2, Upload } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { ACCEPTED_DOCUMENT_TYPES, MAX_OTHER_DOCUMENTS, REQUIRED_DOCUMENTS, checkDocumentFile, openAgentDocument } from "@/lib/agent/documents";
import { deleteAgentDocument, updateBusinessLicense, uploadAgentDocument } from "@/lib/api/agent";
import { errorMessage } from "@/lib/api/errors";
import { applyApiError } from "@/lib/form/applyApiError";
import { formatDateTime, formatFileSize } from "@/lib/format";
import type { AgentDocumentResponse, AgentDocumentType, AgentProfileResponse } from "@/types/agent";
import { ChangeRequestButton } from "./ChangeRequestButton";
import { useSetAgentProfile } from "./useAgentProfile";

export function DocumentsSection({ profile, editable }: { profile: AgentProfileResponse; editable: boolean }) {
  const byType = (type: AgentDocumentType) => profile.documents.filter((d) => d.type === type);
  const others = byType("OTHER");

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Giấy tờ pháp lý</CardTitle>
        <CardDescription>
          File PDF, JPG hoặc PNG, tối đa 10 MB. Giấy tờ được lưu riêng tư — chỉ bạn và quản trị viên xem được.
        </CardDescription>
        {profile.status === "APPROVED" && (
          <CardAction>
            <ChangeRequestButton profile={profile} />
          </CardAction>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {REQUIRED_DOCUMENTS.map(({ type, label, hint }) => (
          <DocumentRow key={type} type={type} label={label} hint={hint} document={byType(type)[0]} editable={editable}>
            {type === "TRAVEL_LICENSE" && <LicenseNumberField profile={profile} editable={editable} />}
          </DocumentRow>
        ))}

        <div className="mt-2 rounded-2xl border border-dashed p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="font-medium">Giấy tờ khác (không bắt buộc)</p>
              <p className="text-sm text-muted-foreground">
                Tối đa {MAX_OTHER_DOCUMENTS} file, ví dụ: chứng chỉ, hợp đồng đối tác, giải thưởng.
              </p>
            </div>
            {editable && others.length < MAX_OTHER_DOCUMENTS && <UploadButton type="OTHER" label="Thêm giấy tờ" icon="add" />}
          </div>
          {others.length > 0 && (
            <ul className="mt-3 flex flex-col gap-2">
              {others.map((document) => (
                <li key={document.id} className="flex items-center justify-between gap-2 rounded-xl bg-muted/50 px-3 py-2">
                  <FileMeta document={document} />
                  <DocumentActions document={document} editable={editable} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function DocumentRow({
  type,
  label,
  hint,
  document,
  editable,
  children,
}: {
  type: AgentDocumentType;
  label: string;
  hint: string;
  document?: AgentDocumentResponse;
  editable: boolean;
  children?: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          {document ? (
            <CircleCheck className="mt-0.5 size-5 shrink-0 text-emerald-600" aria-label="Đã tải lên" />
          ) : (
            <CircleDashed className="mt-0.5 size-5 shrink-0 text-muted-foreground" aria-label="Chưa tải lên" />
          )}
          <div>
            <p className="font-medium">
              {label} <span className="text-destructive">*</span>
            </p>
            {document ? <FileMeta document={document} /> : <p className="text-sm text-muted-foreground">{hint}</p>}
          </div>
        </div>
        <div className="flex items-center gap-2">
          {document && <DocumentActions document={document} editable={false} />}
          {editable && <UploadButton type={type} label={document ? "Thay file" : "Tải lên"} icon="upload" />}
        </div>
      </div>
      {children}
    </div>
  );
}

function FileMeta({ document }: { document: AgentDocumentResponse }) {
  return (
    <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
      <FileText className="size-3.5 shrink-0" />
      <span className="max-w-56 truncate">{document.originalFilename ?? "Tệp đính kèm"}</span>
      <span>· {formatFileSize(document.sizeBytes)} · {formatDateTime(document.uploadedAt)}</span>
    </p>
  );
}

function DocumentActions({ document, editable }: { document: AgentDocumentResponse; editable: boolean }) {
  const { refetch } = useSetAgentProfile();
  const [confirming, setConfirming] = useState(false);

  return (
    <div className="flex items-center gap-1">
      <Button variant="ghost" size="sm" className="rounded-lg" onClick={() => openAgentDocument(document.id)}>
        <Eye /> Xem
      </Button>
      {editable && (
        <>
          <Button variant="ghost" size="sm" className="rounded-lg text-destructive" onClick={() => setConfirming(true)}>
            <Trash2 /> Xóa
          </Button>
          <ConfirmDialog
            open={confirming}
            onOpenChange={setConfirming}
            title="Xóa giấy tờ này?"
            description={`"${document.originalFilename ?? document.typeLabel}" sẽ bị xóa vĩnh viễn.`}
            confirmLabel="Xóa"
            destructive
            onConfirm={async () => {
              try {
                await deleteAgentDocument(document.id);
                await refetch();
                toast.success("Đã xóa giấy tờ");
              } catch (error) {
                toast.error(errorMessage(error));
                throw error;
              }
            }}
          />
        </>
      )}
    </div>
  );
}

function UploadButton({ type, label, icon }: { type: AgentDocumentType; label: string; icon: "upload" | "add" }) {
  const { refetch } = useSetAgentProfile();
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    const problem = checkDocumentFile(file);
    if (problem) {
      toast.error(problem);
      return;
    }
    setUploading(true);
    try {
      await uploadAgentDocument(type, file);
      await refetch();
      toast.success("Đã tải lên giấy tờ");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = ""; // cho phép chọn lại đúng file đó
    }
  };

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_DOCUMENT_TYPES.join(",")}
        className="hidden"
        onChange={(e) => handleFile(e.target.files?.[0])}
      />
      <Button variant="outline" size="sm" className="rounded-lg" disabled={uploading} onClick={() => inputRef.current?.click()}>
        {uploading ? <Spinner /> : icon === "add" ? <Plus /> : <Upload />}
        {uploading ? "Đang tải..." : label}
      </Button>
    </>
  );
}

const licenseSchema = z.object({
  businessLicense: z.string().trim().min(1, "Nhập số giấy phép in trên giấy phép lữ hành").max(255, "Tối đa 255 ký tự"),
});

/** Số giấy phép lữ hành — đặt ngay cạnh file giấy phép để Admin dễ đối chiếu. */
function LicenseNumberField({ profile, editable }: { profile: AgentProfileResponse; editable: boolean }) {
  const { set } = useSetAgentProfile();
  const form = useForm<z.infer<typeof licenseSchema>>({
    resolver: zodResolver(licenseSchema),
    defaultValues: { businessLicense: profile.businessLicense ?? "" },
  });

  if (!editable) {
    return (
      <p className="mt-3 border-t pt-3 text-sm">
        <span className="text-muted-foreground">Số giấy phép: </span>
        <span className="font-medium">{profile.businessLicense ?? "Chưa có"}</span>
      </p>
    );
  }

  const onSubmit = async (values: z.infer<typeof licenseSchema>) => {
    try {
      const updated = await updateBusinessLicense(values.businessLicense);
      set(updated);
      form.reset({ businessLicense: updated.businessLicense ?? "" });
      toast.success("Đã lưu số giấy phép");
    } catch (error) {
      applyApiError(error, form.setError);
    }
  };

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)} className="mt-3 flex flex-col gap-2 border-t pt-3 sm:flex-row sm:items-start">
      <div className="flex-1">
        <TextField
          control={form.control}
          name="businessLicense"
          label="Số giấy phép lữ hành"
          placeholder="Ví dụ: 01-123/2024/TCDL-GP LHQT"
        />
      </div>
      <Button
        type="submit"
        variant="secondary"
        className="h-12 rounded-xl sm:mt-7"
        disabled={form.formState.isSubmitting || !form.formState.isDirty}
      >
        {form.formState.isSubmitting && <Spinner />}
        Lưu
      </Button>
    </form>
  );
}
