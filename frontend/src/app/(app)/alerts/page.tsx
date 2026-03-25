"use client";

import { useEffect, useState } from "react";
import { Section } from "@/components/ui/section";
import { AlertsList } from "@/features/alerts/components/alerts-list";
import { fetchAlerts, markAlertAsRead } from "@/features/alerts/api";
import { AlertItem } from "@/features/alerts/types";

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdatingId, setIsUpdatingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const loadAlerts = async (mountedRef?: { current: boolean }) => {
    setError(null);
    try {
      const alertsData = await fetchAlerts();
      const sorted = [...alertsData].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      if (mountedRef && !mountedRef.current) return;
      setAlerts(sorted);
    } catch (err) {
      const message = err instanceof Error ? err.message : "No se pudieron cargar las alertas.";
      if (mountedRef && !mountedRef.current) return;
      setError(message);
    }
  };

  useEffect(() => {
    const mounted = { current: true };
    async function initialize() {
      setIsLoading(true);
      await loadAlerts(mounted);
      if (mounted.current) setIsLoading(false);
    }
    void initialize();
    return () => {
      mounted.current = false;
    };
  }, []);

  const onMarkAsRead = async (alertId: number) => {
    setError(null);
    setSuccess(null);
    setIsUpdatingId(alertId);
    try {
      await markAlertAsRead(alertId);
      await loadAlerts();
      setSuccess("La alerta fue marcada como leída.");
    } catch (err) {
      const message = err instanceof Error ? err.message : "No se pudo actualizar la alerta.";
      setError(message);
    } finally {
      setIsUpdatingId(null);
    }
  };

  return (
    <div className="dashboard-grid">
      <Section title="Alertas" subtitle="Historial completo de eventos de glucosa">
        {success ? <p className="success-text">{success}</p> : null}
        <AlertsList
          alerts={alerts}
          isLoading={isLoading}
          error={error}
          isUpdatingId={isUpdatingId}
          onMarkAsRead={onMarkAsRead}
        />
      </Section>
    </div>
  );
}

