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
// Browseren skal spørge efter ny index.html og CSS hver gang. Ellers peger en
// gammel index.html på Blazor-filer, der ikke findes længere efter et nyt build.
var noCache = new StaticFileOptions
{
    OnPrepareResponse = ctx =>
    {
        if (ctx.File.Name.EndsWith(".css") || ctx.File.Name == "index.html")
        {
            ctx.Context.Response.Headers.CacheControl = "no-cache";
        }
    },
};
app.UseStaticFiles(noCache);

app.MapOrderEndpoints();
app.MapFallbackToFile("index.html", noCache);

app.Run();
