export interface Organization {
  id: number;
  name: string;
  apiKey: string;
  createdAt: string;
}

export interface AuthResponse {
  organizationId: number;
  organizationName: string;
  email: string | null;
  apiKey: string | null;
}

export interface Endpoint {
  id: number;
  name: string;
  url: string;
  secret: string;
  rateLimitPerSecond: number;
  verified: boolean;
  verifiedAt: string | null;
  createdAt: string;
}

export interface Topic {
  id: number;
  name: string;
  createdAt: string;
}

export interface Subscription {
  id: number;
  topicId: number;
  endpointId: number;
  createdAt: string;
}

export type DeliveryStatus = "PENDING" | "SUCCESS" | "FAILED";

export interface Delivery {
  id: number;
  eventId: number;
  endpointId: number;
  topic: string;
  payload: string;
  status: DeliveryStatus;
  attemptCount: number;
  maxAttempts: number;
  nextAttemptAt: string;
  lastAttemptAt: string | null;
  lastResponseStatus: number | null;
  createdAt: string;
}

export interface AuditLog {
  id: number;
  action: string;
  details: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
