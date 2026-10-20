namespace Delivery.Contracts;

// REST-kontrakten mellem browseren og app-serveren. Status sendes som tekst
// (fx "ASSIGNED"), så klienten ikke kender gRPC-enum'ens tal.

public record OrderDto(
    string Id,
    string Status,
    string WarehouseId,
    string Customer,
    string Product,
    string DeliveryWindowStart,
    string DeliveryWindowEnd,
    long Version);

public record TransitionsDto(string OrderId, IReadOnlyList<string> Allowed);

public record TransitionRequestDto(string ToStatus, long ExpectedVersion, string? Note);
