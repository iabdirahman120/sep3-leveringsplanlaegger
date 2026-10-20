using System.Net.Http.Json;
using Delivery.Contracts;

namespace Delivery.Client.Services;

// Klientens eneste vej til serveren. Fejl fra app-serveren bliver til en
// ApiException med Javas forklaring, så siden kan vise den.
public class OrderApi(HttpClient http)
{
    public async Task<List<OrderDto>> GetOrdersAsync() =>
        await http.GetFromJsonAsync<List<OrderDto>>("api/orders") ?? [];

    public async Task<List<string>> GetAllowedAsync(string orderId)
    {
        var response = await http.GetAsync($"api/orders/{orderId}/transitions");
        await EnsureSuccess(response);
        var dto = await response.Content.ReadFromJsonAsync<TransitionsDto>();
        return dto?.Allowed.ToList() ?? [];
    }

    public async Task<OrderDto> TransitionAsync(string orderId, string toStatus, long expectedVersion)
    {
        var response = await http.PostAsJsonAsync($"api/orders/{orderId}/transition",
            new TransitionRequestDto(toStatus, expectedVersion, null));
        await EnsureSuccess(response);
        return (await response.Content.ReadFromJsonAsync<OrderDto>())!;
    }

    private static async Task EnsureSuccess(HttpResponseMessage response)
    {
        if (response.IsSuccessStatusCode)
        {
            return;
        }
        var problem = await response.Content.ReadFromJsonAsync<ProblemDto>();
        throw new ApiException((int)response.StatusCode, problem?.Detail ?? response.ReasonPhrase ?? "Ukendt fejl");
    }

    private record ProblemDto(string? Detail);
}

public class ApiException(int statusCode, string message) : Exception(message)
{
    public int StatusCode { get; } = statusCode;
}
