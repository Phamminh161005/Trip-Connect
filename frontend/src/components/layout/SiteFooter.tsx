import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="border-t bg-muted/40">
      <div className="mx-auto flex max-w-7xl flex-col gap-2 px-4 py-6 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-10">
        <span>© {new Date().getFullYear()} TripConnect</span>
        <Link href="/credits" className="underline-offset-4 hover:underline">
          Nguồn hình ảnh
        </Link>
      </div>
    </footer>
  );
}
