using Grpc.Core;
using Microsoft.AspNetCore.Diagnostics;

namespace Delivery.Bff.Orders;

// Oversætter gRPC-statuskoder fra Java til HTTP-statuskoder til browseren.
// Beskeden fra Java sendes med, så klienten kan vise hvorfor.
public class GrpcErrorHandler : IExceptionHandler
{
    public async ValueTask<bool> TryHandleAsync(HttpContext context, Exception exception, CancellationToken cancellationToken)
    {
        if (exception is not RpcException rpc)
        {
            return false;
        }

        var statusCode = rpc.StatusCode switch
        {
            StatusCode.NotFound => StatusCodes.Status404NotFound,
            StatusCode.InvalidArgument => StatusCodes.Status400BadRequest,
            StatusCode.FailedPrecondition => StatusCodes.Status422UnprocessableEntity,
            StatusCode.Aborted => StatusCodes.Status409Conflict,
            StatusCode.Unavailable => StatusCodes.Status503ServiceUnavailable,
            _ => StatusCodes.Status500InternalServerError
        };
        var detail = rpc.StatusCode == StatusCode.Unavailable
            ? "Domæneserveren svarer ikke"
            : rpc.Status.Detail;

        context.Response.StatusCode = statusCode;
        await context.Response.WriteAsJsonAsync(new { status = statusCode, detail }, cancellationToken);
        return true;
    }
}
