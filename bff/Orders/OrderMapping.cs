using Delivery.Contracts;
using Delivery.V1;

namespace Delivery.Bff.Orders;

public static class OrderMapping
{
    // Samme navne som i proto-filen uden præfikset ORDER_STATUS_.
    private static readonly Dictionary<OrderStatus, string> Names = new()
    {
        [OrderStatus.Pending] = "PENDING",
        [OrderStatus.Assigned] = "ASSIGNED",
        [OrderStatus.PickedUp] = "PICKED_UP",
        [OrderStatus.InTransit] = "IN_TRANSIT",
        [OrderStatus.Delivered] = "DELIVERED",
        [OrderStatus.Failed] = "FAILED",
        [OrderStatus.Cancelled] = "CANCELLED"
    };

    public static OrderDto ToDto(Order order) => new(
        order.Id,
        ToText(order.Status),
        order.WarehouseId,
        order.CustomerId,
        order.Items.FirstOrDefault()?.ProductName ?? "",
        order.DeliveryWindowStart,
        order.DeliveryWindowEnd,
        order.Version);

    public static string ToText(OrderStatus status) =>
        Names.TryGetValue(status, out var name) ? name : "UNSPECIFIED";

    public static bool TryParse(string text, out OrderStatus status)
    {
        var match = Names.FirstOrDefault(pair => pair.Value == text.ToUpperInvariant());
        status = match.Value is null ? OrderStatus.Unspecified : match.Key;
        return match.Value is not null;
    }
}
