# Quick Start Guide - Complaints MCP Server

## Prerequisites

1. **PostgreSQL** running on localhost:5432
2. **Database** named `mydb` with credentials:
   - Username: `admin`
   - Password: `admin`

### Quick PostgreSQL Setup (if needed)

```bash
# Using Docker
docker run --name postgres-mcp \
  -e POSTGRES_DB=mydb \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -p 5432:5432 \
  -d postgres:16

# Or create the database if PostgreSQL is already running
createdb -U admin mydb
```

## Start the Server

```bash
./mvnw quarkus:dev
```

The server will:
- Start on port 8080
- Automatically create the `complaints` table
- Insert sample data
- Expose MCP tools at `http://localhost:8080/mcp`
- Expose SSE endpoint at `http://localhost:8080/mcp/sse`

## Test the MCP Server

### Option 1: Automated Test Script

```bash
./test-mcp.sh
```

This will run all MCP tool tests and display results.

### Option 2: Manual Testing with curl

**List all available tools:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }' | jq '.'
```

**Search Electronics complaints:**
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
  }' | jq '.'
```

**Get complaint statistics by category:**
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
  }' | jq '.'
```

## Available MCP Tools

| Tool Name | Description |
|-----------|-------------|
| `searchComplaints` | Search with filters: category, productName, complaintText, fromDate, toDate, sortByRating |
| `getComplaintById` | Get a specific complaint by ID |
| `getComplaintStatsByCategory` | Get aggregated statistics grouped by category |
| `getComplaintStatsByProduct` | Get aggregated statistics grouped by product |
| `getLowRatedComplaints` | Get complaints with rating ≤ 2 |

## Date Filtering Examples

**Get complaints from last week:**
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
        "fromDate": "2026-03-24",
        "toDate": "2026-03-31"
      }
    }
  }' | jq '.'
```

**Search with multiple filters:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "searchComplaints",
      "arguments": {
        "category": "Electronics",
        "fromDate": "2026-03-01",
        "complaintText": "battery",
        "sortByRating": "asc"
      }
    }
  }' | jq '.'
```

## REST API (Alternative Access)

You can also access the data via REST endpoints:

```bash
# Get all complaints
curl http://localhost:8080/complaints | jq '.'

# Filter by category
curl "http://localhost:8080/complaints?category=Electronics" | jq '.'

# Filter by date range
curl "http://localhost:8080/complaints?fromDate=2026-03-01&toDate=2026-03-31" | jq '.'

# Get by ID
curl http://localhost:8080/complaints/C001 | jq '.'
```

## Connect with Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "complaints-db": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

Restart Claude Desktop and ask:
- "Show me all Electronics complaints sorted by rating"
- "What products have the most complaints?"
- "Find complaints about battery issues"
- "Give me statistics by category"

## Sample Data

The server comes with 8 sample complaints across categories:
- **Electronics**: Laptops, Monitors (5 complaints)
- **Accessories**: Mouse, Keyboard, USB Cable (3 complaints)
- **Furniture**: Office Chair (1 complaint)

Ratings range from 1-5 (1 = worst, 5 = excellent)

## Troubleshooting

**Port 8080 already in use:**
```bash
# Change port in application.properties
quarkus.http.port=8081
```

**Database connection issues:**
```bash
# Check PostgreSQL is running
psql -U admin -d mydb -c "SELECT 1"

# Check credentials in src/main/resources/application.properties
```

**MCP tools not showing:**
```bash
# Check logs for errors
./mvnw quarkus:dev

# Verify tools endpoint
curl http://localhost:8080/mcp -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## Next Steps

- Read [README-MCP.md](README-MCP.md) for detailed MCP documentation
- Read [README-COMPLAINTS.md](README-COMPLAINTS.md) for API documentation
- Customize the `ComplaintMcpTools.java` to add your own tools
- Modify `import.sql` to load your own data
