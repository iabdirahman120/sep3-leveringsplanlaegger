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

// App-serveren hoster Blazor-klienten, så browseren kun taler med én adresse.
if (app.Environment.IsDevelopment())
{
    app.UseWebAssemblyDebugging();
}
app.UseBlazorFrameworkFiles();
app.UseStaticFiles();

app.MapOrderEndpoints();
app.MapFallbackToFile("index.html");

app.Run();
