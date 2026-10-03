"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { TextField } from "@/components/form/TextField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { updateMe } from "@/lib/api/auth";
import { useAuth } from "@/lib/auth/AuthProvider";
import { applyApiError } from "@/lib/form/applyApiError";
import { profileSchema, type ProfileValues } from "@/lib/validation/auth";
import type { UserRole } from "@/types/auth";

const ROLE_LABELS: Record<UserRole, string> = {
  CUSTOMER: "Khách du lịch",
  AGENT: "Đối tác",
  ADMIN: "Quản trị viên",
};

export function ProfileForm() {
  const { user, reloadUser } = useAuth();

  const form = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: user?.fullName ?? "", phone: user?.phone ?? "" },
  });

  if (!user) return null; // RequireAuth đảm bảo đã có user

  const onSubmit = async (values: ProfileValues) => {
    try {
      await updateMe(values);
      await reloadUser();
      form.reset(values); // đánh dấu form "chưa thay đổi" để tắt nút Lưu
      toast.success("Đã cập nhật thông tin cá nhân");
    } catch (error) {
      applyApiError(error, form.setError);
    }
  };

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="flex items-center gap-3 text-xl">
          Thông tin cá nhân <Badge variant="secondary">{ROLE_LABELS[user.role]}</Badge>
        </CardTitle>
        <CardDescription>Thông tin này được dùng khi bạn đặt tour và liên hệ với đối tác.</CardDescription>
      </CardHeader>
      <CardContent>
        <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
          <FieldGroup className="gap-5">
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input id="email" value={user.email} disabled className="h-12 rounded-xl text-base" />
              <FieldDescription>Email dùng để đăng nhập, không thể thay đổi.</FieldDescription>
            </Field>
            <TextField control={form.control} name="fullName" label="Họ và tên" autoComplete="name" />
            <TextField control={form.control} name="phone" label="Số điện thoại" type="tel" autoComplete="tel" />
            <div>
              <Button
                type="submit"
                size="lg"
                className="h-11 rounded-xl px-6 font-semibold"
                disabled={form.formState.isSubmitting || !form.formState.isDirty}
              >
                {form.formState.isSubmitting && <Spinner />}
                Lưu thay đổi
              </Button>
            </div>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  );
}
