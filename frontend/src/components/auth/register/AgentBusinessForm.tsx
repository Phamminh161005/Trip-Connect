"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AgentBusinessFields } from "@/components/agent/AgentBusinessFields";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { agentBusinessSchema, type AgentBusinessValues } from "@/lib/validation/auth";

interface AgentBusinessFormProps {
  defaultValues?: Partial<AgentBusinessValues>;
  onBack: () => void;
  onNext: (values: AgentBusinessValues) => void;
}

/** Bước "Thông tin doanh nghiệp" khi đăng ký Agent. Giấy tờ pháp lý (file) được tải lên SAU khi xác thực email. */
export function AgentBusinessForm({ defaultValues, onBack, onNext }: AgentBusinessFormProps) {
  const form = useForm<AgentBusinessValues>({
    resolver: zodResolver(agentBusinessSchema),
    defaultValues: {
      companyName: "",
      taxCode: "",
      address: "",
      bankBin: "",
      bankAccountNumber: "",
      bankAccountHolder: "",
      ...defaultValues,
    },
  });

  return (
    <form noValidate onSubmit={form.handleSubmit(onNext)}>
      <FieldGroup className="gap-6">
        <AgentBusinessFields control={form.control} autoFocus />
        <p className="rounded-xl bg-accent/60 px-4 py-3 text-sm text-accent-foreground">
          Sau khi xác thực email, bạn sẽ tải lên giấy tờ pháp lý (giấy phép lữ hành, đăng ký kinh doanh, CCCD người
          đại diện) để gửi hồ sơ cho quản trị viên duyệt.
        </p>
        <div className="flex gap-3">
          <Button type="button" variant="outline" size="lg" className="h-12 flex-1 rounded-xl" onClick={onBack}>
            Quay lại
          </Button>
          <Button type="submit" size="lg" className="h-12 flex-1 rounded-xl text-base font-semibold">
            Tiếp tục
          </Button>
        </div>
      </FieldGroup>
    </form>
  );
}
