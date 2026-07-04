from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .schemas import ExecutionRequest, ExecutionResponse
from .executor import CodeExecutor

app = FastAPI(
    title="Code Executor Service",
    description="Servis za kompilaciju i izvršavanje C, C# i Python koda",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

executor = CodeExecutor()


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "Code Executor"}


@app.post("/api/execute", response_model=ExecutionResponse)
async def execute_code(request: ExecutionRequest):
    """Kompilira i izvršava kod."""
    try:
        result = executor.execute(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/")
async def root():
    return {
        "service": "Code Executor",
        "version": "1.0.0",
        "supported_languages": ["C", "CSHARP", "PYTHON"],
        "endpoints": {
            "health": "/health",
            "execute": "POST /api/execute"
        }
    }