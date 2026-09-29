export class RelayApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "RelayApiError";
    this.status = status;
  }
}
