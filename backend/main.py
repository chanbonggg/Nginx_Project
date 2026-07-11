from fastapi import FastAPI

app = FastAPI()

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": "backend"
    }

@app.get("/")
def root():
    return {
        "message": "FastAPI backend is running"
    }