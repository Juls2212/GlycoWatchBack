"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
  { href: "/dashboard", label: "Panel" },
  { href: "/measurements", label: "Mediciones" },
  { href: "/alerts", label: "Alertas" },
  { href: "/devices", label: "Dispositivos" },
  { href: "/profile", label: "Perfil" },
  { href: "/analytics", label: "Análisis" }
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-dot" />
        <div>
          <p className="brand-title">GlycoWatch</p>
          <p className="brand-subtitle">Centro analítico</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link key={item.label} href={item.href} className={`nav-link ${active ? "active" : ""}`}>
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
