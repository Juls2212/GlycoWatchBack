import { Card } from "@/components/ui/card";
import { FeedbackBanner } from "@/components/ui/feedback-banner";
import { SkeletonBlock } from "@/components/ui/skeleton-block";
import { AlertItem } from "@/features/alerts/types";

type Props = {
  alerts: AlertItem[];
  isLoading: boolean;
  error: string | null;
  isUpdatingId: number | null;
  onMarkAsRead: (alertId: number) => Promise<void>;
};

function formatAlertType(type: AlertItem["type"]): string {
  return type === "HIGH_GLUCOSE" ? "Glucosa alta" : "Glucosa baja";
}

function resolveSeverityClass(type: AlertItem["type"]): string {
  return type === "HIGH_GLUCOSE" ? "high" : "low";
}

export function AlertsList({ alerts, isLoading, error, isUpdatingId, onMarkAsRead }: Props) {
  return (
    <Card>
      {isLoading ? (
        <div className="skeleton-stack">
          <SkeletonBlock className="skeleton-line w-50" />
          <SkeletonBlock className="skeleton-line w-100" />
          <SkeletonBlock className="skeleton-line w-90" />
          <SkeletonBlock className="skeleton-line w-80" />
        </div>
      ) : null}
      {error ? <FeedbackBanner type="error" message={error} /> : null}

      {!isLoading && !error && alerts.length === 0 ? (
        <div className="empty-state">
          <p className="empty-title">Sin alertas registradas</p>
          <p className="soft-text">Cuando se detecten eventos de riesgo, aparecerán aquí.</p>
        </div>
      ) : null}

      {!isLoading && !error && alerts.length > 0 ? (
        <ul className="alerts-list alerts-list-full">
          {alerts.map((alert) => (
            <li key={alert.id} className={`alert-row alert-row-${resolveSeverityClass(alert.type)}`}>
              <div>
                <p className={`alert-type ${resolveSeverityClass(alert.type)}`}>{formatAlertType(alert.type)}</p>
                <p className="alert-message">{alert.message}</p>
                <p className="soft-text">Fecha: {new Date(alert.createdAt).toLocaleString("es-CO")}</p>
              </div>

              <div className="alerts-actions">
                <div className={`alert-badge ${alert.isRead ? "read" : "unread"}`}>
                  {alert.isRead ? "Leída" : "No leída"}
                </div>
                <button
                  type="button"
                  className="ghost-button"
                  disabled={alert.isRead || isUpdatingId === alert.id}
                  onClick={() => void onMarkAsRead(alert.id)}
                >
                  {isUpdatingId === alert.id ? "Guardando..." : "Marcar como leída"}
                </button>
              </div>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}
