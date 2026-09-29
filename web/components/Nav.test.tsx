import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Nav } from "./Nav";
import { clearStoredApiKey, setStoredApiKey } from "@/lib/apiKey";
import { clearStoredSession, setStoredSession } from "@/lib/session";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  usePathname: () => "/endpoints",
  useRouter: () => ({ push }),
}));

vi.mock("@/lib/apiClient", () => ({
  api: { post: vi.fn().mockResolvedValue(undefined) },
}));

describe("Nav", () => {
  beforeEach(() => {
    push.mockClear();
  });

  afterEach(() => {
    clearStoredApiKey();
    clearStoredSession();
    vi.clearAllMocks();
  });

  it("renders nothing when disconnected", () => {
    const { container } = render(<Nav />);

    expect(container).toBeEmptyDOMElement();
  });

  it("renders the nav links and Disconnect button when an API key is stored", () => {
    setStoredApiKey("relay_test123");

    render(<Nav />);

    expect(screen.getByText("Endpoints")).toBeInTheDocument();
    expect(screen.getByText("Topics")).toBeInTheDocument();
    expect(screen.getByText("Deliveries")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Disconnect" })).toBeInTheDocument();
  });

  it("renders when a dashboard session is stored instead of an API key", () => {
    setStoredSession({ organizationId: 1, organizationName: "Acme", email: "owner@acme.dev" });

    render(<Nav />);

    expect(screen.getByRole("button", { name: "Disconnect" })).toBeInTheDocument();
  });

  it("disconnecting an API-key-only connection clears the key without calling logout", async () => {
    const { api } = await import("@/lib/apiClient");
    setStoredApiKey("relay_test123");
    render(<Nav />);

    fireEvent.click(screen.getByRole("button", { name: "Disconnect" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
    expect(api.post).not.toHaveBeenCalled();
    expect(localStorage.getItem("relay.apiKey")).toBeNull();
  });

  it("disconnecting a session calls /auth/logout and clears the session", async () => {
    const { api } = await import("@/lib/apiClient");
    setStoredSession({ organizationId: 1, organizationName: "Acme", email: "owner@acme.dev" });
    render(<Nav />);

    fireEvent.click(screen.getByRole("button", { name: "Disconnect" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
    expect(api.post).toHaveBeenCalledWith("/auth/logout");
    expect(localStorage.getItem("relay.session")).toBeNull();
  });
});
