# Complaints MCP Server

A Quarkus-based MCP (Model Context Protocol) server that exposes complaint database operations as tools for AI assistants.

## MCP Tools Available

### 1. `searchComplaints`
Search and filter customer complaints with multiple criteria.

**Parameters:**
- `category` (optional): Filter by exact category (e.g., 'Electronics', 'Furniture', 'Accessories')
- `productName` (optional): Search by product name (partial match, case-insensitive)
- `complaintText` (optional): Search within complaint text (partial match, case-insensitive)
- `fromDate` (optional): Filter complaints from this date (format: YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)
- `toDate` (optional): Filter complaints until this date (format: YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)
- `sortByRating` (optional): Sort by rating ('asc' or 'desc')

**Example:**
```json
{
  "category": "Electronics",
  "fromDate": "2026-03-01",
  "toDate": "2026-03-31",
  "sortByRating": "asc"
}
```

### 2. `getComplaintById`
Get a specific complaint by its ID.

**Parameters:**
- `id` (required): The complaint ID (e.g., "C001")

**Example:**
```json
{
  "id": "C001"
}
```

### 3. `getComplaintStatsByCategory`
Get aggregated statistics for complaints grouped by category.

**Returns:**
- Total complaints per category
- Average rating per category
- Rating distribution per category

**No parameters required**

### 4. `getComplaintStatsByProduct`
Get aggregated statistics for complaints grouped by product.

**Parameters:**
- `minComplaintCount` (optional): Minimum number of complaints to include a product

**Returns:**
- Products sorted by complaint count
- Total complaints per product
- Average rating per product
- Lowest rating per product

**Example:**
```json
{
  "minComplaintCount": 2
}
```

### 5. `getLowRatedComplaints`
Get complaints with rating 1 or 2 that need immediate attention.

**Parameters:**
- `category` (optional): Filter by category

**Returns:**
- Low-rated complaints (rating ≤ 2)
- Sorted by rating (lowest first) and date (newest first)

**Example:**
```json
{
  "category": "Electronics"
}
```

## Running the MCP Server

### Prerequisites
- PostgreSQL running on localhost:5432
- Database named `mydb` with user `admin` and password `admin`

### Start the Server

```bash
./mvnw quarkus:dev
```

The MCP server will be available at:
- **HTTP endpoint**: `http://localhost:8080/mcp`
- **SSE endpoint**: `http://localhost:8080/mcp/sse`

## Testing the MCP Server

### Using curl

**List available tools:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'
```

**Call searchComplaints tool:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "searchComplaints",
      "arguments": {
        "category": "Electronics",
        "sortByRating": "asc"
      }
    }
  }'
```

**Call getComplaintStatsByCategory tool:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "getComplaintStatsByCategory",
      "arguments": {}
    }
  }'
```

**Search complaints by date range:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "searchComplaints",
      "arguments": {
        "fromDate": "2026-03-01",
        "toDate": "2026-03-31",
        "complaintText": "battery"
      }
    }
  }'
```

**Get low-rated complaints:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "getLowRatedComplaints",
      "arguments": {
        "category": "Electronics"
      }
    }
  }'
```

## Connecting with Claude Desktop

Add to your Claude Desktop configuration (`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "complaints-db": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

After restarting Claude Desktop, you can ask:
- "Show me all complaints about Laptops"
- "What are the low-rated Electronics complaints?"
- "Give me complaint statistics by category"
- "Find complaints from the last week with the word 'battery'"

## REST API Endpoints

The REST API is still available alongside the MCP server:

```bash
# Get all complaints
curl http://localhost:8080/complaints

# Filter by date range
curl "http://localhost:8080/complaints?fromDate=2026-03-01&toDate=2026-03-31"

# Combine multiple filters
curl "http://localhost:8080/complaints?category=Electronics&fromDate=2026-03-01&sortByRating=asc"
```

## Architecture

```
┌─────────────────────┐
│   MCP Client        │
│ (Claude Desktop,    │
│  AI Assistants)     │
└──────────┬──────────┘
           │ MCP Protocol
           │ (HTTP/SSE)
           ▼
┌─────────────────────┐
│ ComplaintMcpTools   │
│   @Tool methods     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ ComplaintService    │
│   JDBC Logic        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   PostgreSQL DB     │
│  complaints table   │
└─────────────────────┘
```

## Configuration

Update `src/main/resources/application.properties`:

```properties
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=admin
quarkus.datasource.password=admin
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb

# MCP Server
quarkus.mcp.server.server-info.name=JDBC Demo
quarkus.mcp.server.server-info.description=JDBC Demo Server
quarkus.mcp.server.traffic-logging.enabled=true
quarkus.mcp.server.traffic-logging.text-limit=1000
```

## Sample Use Cases

1. **Customer Support Dashboard**: Query low-rated complaints for immediate attention
2. **Product Quality Analysis**: Track complaints by product over time
3. **Trend Analysis**: Search complaints by date range and keywords
4. **Category Insights**: Get statistics on which product categories have the most issues
5. **AI-Powered Support**: Enable AI assistants to answer questions about customer complaints

## Date Filtering Examples

**Last 7 days:**
```json
{
  "fromDate": "2026-03-24",
  "toDate": "2026-03-31"
}
```

**Specific month:**
```json
{
  "fromDate": "2026-03-01",
  "toDate": "2026-03-31"
}
```

**Before a specific date:**
```json
{
  "toDate": "2026-03-15"
}
```

**After a specific date:**
```json
{
  "fromDate": "2026-03-15"
}
```

**With time:**
```json
{
  "fromDate": "2026-03-31 00:00:00",
  "toDate": "2026-03-31 23:59:59"
}
```
