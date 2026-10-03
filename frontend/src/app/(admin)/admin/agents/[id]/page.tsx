import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { AgentReviewView } from "./AgentReviewView";

export const metadata: Metadata = { title: "Xét duyệt hồ sơ đối tác" };

export default async function AdminAgentDetailPage({ params }: PageProps<"/admin/agents/[id]">) {
  const id = Number((await params).id);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <AgentReviewView id={id} />;
}
