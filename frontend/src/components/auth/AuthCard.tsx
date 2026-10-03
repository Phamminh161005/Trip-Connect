import type { ReactNode } from "react";
import { LogoMark } from "@/components/brand/Logo";
import { cn } from "@/lib/utils";

interface AuthCardProps {
  title: string;
  description?: ReactNode;
  children: ReactNode;
  /** Thẻ rộng hơn cho form nhiều trường (đăng ký Agent). */
  wide?: boolean;
  footer?: ReactNode;
}

export function AuthCard({ title, description, children, wide, footer }: AuthCardProps) {
  return (
    <section
      className={cn(
        "mx-auto w-full rounded-[2rem] border bg-card px-6 py-8 shadow-xl sm:px-10 sm:py-10",
        wide ? "max-w-2xl" : "max-w-md",
      )}
    >
      <div className="mb-7 flex flex-col items-center gap-3 text-center">
        <LogoMark className="size-11" />
        <h1 className="text-2xl font-bold tracking-tight sm:text-[1.7rem]">{title}</h1>
        {description && <p className="text-sm text-balance text-muted-foreground">{description}</p>}
      </div>
      {children}
      {footer && <div className="mt-6 text-center text-sm text-muted-foreground">{footer}</div>}
    </section>
  );
}
