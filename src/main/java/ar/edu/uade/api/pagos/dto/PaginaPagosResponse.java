package ar.edu.uade.api.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// DTO-06
@Schema(description = "Pagina de pagos")
public class PaginaPagosResponse {

    private final List<PagoResponse> content;

    @Schema(description = "Numero de pagina (base 0)", example = "0")
    private final int page;

    @Schema(example = "20")
    private final int size;

    private final long totalElements;
    private final int totalPages;

    public PaginaPagosResponse(List<PagoResponse> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<PagoResponse> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
