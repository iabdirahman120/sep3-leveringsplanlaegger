using Delivery.Contracts;
using Delivery.V1;
using Grpc.Core;

namespace Delivery.Bff.Orders;

// REST udadtil, gRPC indadtil. Ingen statusregler her: hvilke overgange der
// er tilladt, spørger vi Java om, og Java tjekker igen ved skrivning.
public static class OrderEndpoints
{
    public static void MapOrderEndpoints(this WebApplication app)
    {
        var orders = app.MapGroup("/api/orders");

        orders.MapGet("/", async (OrderService.OrderServiceClient grpc) =>
        {
            var response = await grpc.ListOrdersAsync(new ListOrdersRequest());
            return Results.Ok(response.Orders.Select(OrderMapping.ToDto));
        });

        orders.MapGet("/{id}/transitions", async (string id, OrderService.OrderServiceClient grpc) =>
        {
            var response = await grpc.GetAllowedTransitionsAsync(new GetAllowedTransitionsRequest { OrderId = id });
            return Results.Ok(new TransitionsDto(id, response.Allowed.Select(OrderMapping.ToText).ToList()));
        });

        orders.MapPost("/{id}/transition", async (string id, TransitionRequestDto body, OrderService.OrderServiceClient grpc) =>
        {
            if (!OrderMapping.TryParse(body.ToStatus, out var toStatus))
            {
                return Results.Problem($"Ukendt status '{body.ToStatus}'", statusCode: StatusCodes.Status400BadRequest);
            }
            var updated = await grpc.TransitionOrderAsync(new TransitionOrderRequest
            {
                OrderId = id,
                ToStatus = toStatus,
                ExpectedVersion = body.ExpectedVersion,
                Note = body.Note ?? ""
            });
            return Results.Ok(OrderMapping.ToDto(updated));
        });
    }
}
