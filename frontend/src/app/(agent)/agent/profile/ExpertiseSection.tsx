"use client";

import { useState } from "react";
import { Pencil } from "lucide-react";
import { toast } from "sonner";
import { AgentExpertiseForm } from "@/components/auth/register/AgentExpertiseForm";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { updateAgentExpertise } from "@/lib/api/agent";
import { errorMessage } from "@/lib/api/errors";
import type { AgentProfileResponse } from "@/types/agent";
import { useSetAgentProfile } from "./useAgentProfile";

/** Khu vực phụ trách + loại hình thế mạnh — sửa được ở MỌI trạng thái (không ảnh hưởng tính pháp lý). */
export function ExpertiseSection({ profile }: { profile: AgentProfileResponse }) {
  const { set } = useSetAgentProfile();
  const [editing, setEditing] = useState(false);

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Khu vực & loại hình tour thế mạnh</CardTitle>
        <CardDescription>Hệ thống dùng thông tin này để phân bổ yêu cầu tư vấn tour phù hợp cho bạn.</CardDescription>
        {!editing && (
          <CardAction>
            <Button variant="outline" size="sm" className="rounded-lg" onClick={() => setEditing(true)}>
              <Pencil /> Chỉnh sửa
            </Button>
          </CardAction>
        )}
      </CardHeader>
      <CardContent>
        {editing ? (
          <div className="flex flex-col gap-3">
            <AgentExpertiseForm
              submitLabel="Lưu thay đổi"
              defaultValues={{
                locationIds: profile.serviceAreas.map((l) => l.id),
                categoryIds: profile.specialties.map((c) => c.id),
              }}
              onSubmit={async (values) => {
                try {
                  set(await updateAgentExpertise(values));
                  toast.success("Đã lưu khu vực và loại hình tour");
                  setEditing(false);
                } catch (error) {
                  toast.error(errorMessage(error));
                }
              }}
            />
            <Button variant="ghost" className="self-start rounded-xl" onClick={() => setEditing(false)}>
              Hủy
            </Button>
          </div>
        ) : (
          <div className="flex flex-col gap-5">
            <ChipGroup title="Khu vực phụ trách" items={profile.serviceAreas.map((l) => ({ id: l.id, name: locationLabel(l) }))} />
            <ChipGroup title="Loại hình tour thế mạnh" items={profile.specialties} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function ChipGroup({ title, items }: { title: string; items: { id: number; name: string }[] }) {
  return (
    <div className="flex flex-col gap-2">
      <p className="text-sm text-muted-foreground">{title}</p>
      {items.length ? (
        <div className="flex flex-wrap gap-1.5">
          {items.map((item) => (
            <Badge key={item.id} variant="secondary" className="px-2.5 py-1">
              {item.name}
            </Badge>
          ))}
        </div>
      ) : (
        <p className="text-sm text-destructive">Chưa chọn</p>
      )}
    </div>
  );
}
