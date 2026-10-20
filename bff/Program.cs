using Delivery.Bff.Orders;
using Delivery.V1;

var builder = WebApplication.CreateBuilder(args);

// gRPC-klient mod Java-domæneserveren. Adressen står i appsettings.json.
builder.Services.AddGrpcClient<OrderService.OrderServiceClient>(options =>
    options.Address = new Uri(builder.Configuration["DomainServer:Address"]!));

builder.Services.AddExceptionHandler<GrpcErrorHandler>();
builder.Services.AddProblemDetails();

var app = builder.Build();

app.UseExceptionHandler();
app.MapOrderEndpoints();

app.Run();
