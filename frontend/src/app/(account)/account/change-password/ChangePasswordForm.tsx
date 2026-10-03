"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { changePassword } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthProvider";
import { applyApiError } from "@/lib/form/applyApiError";
import { changePasswordSchema, type ChangePasswordValues } from "@/lib/validation/auth";

export function ChangePasswordForm() {
  const router = useRouter();
  const { signOut } = useAuth();

  const form = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { oldPassword: "", newPassword: "", confirmPassword: "" },
  });

  const onSubmit = async (values: ChangePasswordValues) => {
    try {
      await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword });
    } catch (error) {
      // Backend trả 400 (không phải 401) khi sai mật khẩu cũ -> hiện ngay dưới ô, KHÔNG đăng xuất người dùng
      if (error instanceof ApiError && error.status === 400 && error.message.includes("Mật khẩu cũ")) {
        form.setError("oldPassword", { type: "server", message: error.message }, { shouldFocus: true });
        return;
      }
      applyApiError(error, form.setError);
      return;
    }

    // Backend đã thu hồi mọi phiên đăng nhập (kể cả phiên hiện tại) -> đăng xuất và đăng nhập lại
    toast.success("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
    await signOut();
    router.replace("/login?reason=password-changed");
  };

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-xl">Đổi mật khẩu</CardTitle>
        <CardDescription>
          Sau khi đổi mật khẩu, bạn sẽ được đăng xuất khỏi mọi thiết bị và cần đăng nhập lại bằng mật khẩu mới.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
          <FieldGroup className="max-w-md gap-5">
            <TextField control={form.control} name="oldPassword" label="Mật khẩu hiện tại" type="password" autoComplete="current-password" />
            <TextField
              control={form.control}
              name="newPassword"
              label="Mật khẩu mới"
              type="password"
              autoComplete="new-password"
              description="Ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số."
            />
            <TextField control={form.control} name="confirmPassword" label="Nhập lại mật khẩu mới" type="password" autoComplete="new-password" />
            <div>
              <Button type="submit" size="lg" className="h-11 rounded-xl px-6 font-semibold" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting && <Spinner />}
                Đổi mật khẩu
              </Button>
            </div>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  );
}
