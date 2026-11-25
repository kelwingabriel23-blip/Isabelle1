"""
Agente da Isabelle para Windows.

Recebe comandos do app Android por WebSocket e executa no PC.
Segurança: exige TOKEN, só aceita comandos de uma lista fixa e só abre
sites (http/https) ou apps que VOCÊ listar em APPS. Use junto com Tailscale
para acessar de fora de casa sem abrir portas no roteador.

Uso:
    pip install websockets
    set ISABELLE_TOKEN=uma-senha-grande-aleatoria      (PowerShell: $env:ISABELLE_TOKEN="...")
    python pc_agent.py
"""
import asyncio
import ctypes
import hmac
import json
import os
import subprocess
import webbrowser

import websockets

TOKEN = os.environ.get("ISABELLE_TOKEN", "")
HOST = "0.0.0.0"
PORT = 8765

# apelido -> caminho do programa. Só o que estiver aqui pode ser aberto por nome.
APPS = {
    "bloco de notas": r"C:\Windows\System32\notepad.exe",
    "calculadora": r"C:\Windows\System32\calc.exe",
    # "chrome": r"C:\Program Files\Google\Chrome\Application\chrome.exe",
}

VK = {  # teclas de mídia do Windows
    "playpause": 0xB3, "next": 0xB0, "prev": 0xB1,
    "volup": 0xAF, "voldown": 0xAE, "mute": 0xAD,
}


def press(vk: int) -> None:
    user32 = ctypes.windll.user32
    user32.keybd_event(vk, 0, 0, 0)
    user32.keybd_event(vk, 0, 2, 0)


def run(cmd: str, args: dict) -> str:
    if cmd == "lock":
        ctypes.windll.user32.LockWorkStation()
    elif cmd == "sleep":
        subprocess.Popen(["rundll32.exe", "powrprof.dll,SetSuspendState", "0,1,0"])
    elif cmd == "shutdown":
        # 60 s para você cancelar com: shutdown /a
        subprocess.Popen(["shutdown", "/s", "/t", "60"])
    elif cmd == "media":
        key = args.get("key", "")
        if key not in VK:
            return "tecla inválida"
        press(VK[key])
    elif cmd == "open":
        target = str(args.get("target", ""))
        if target.lower().startswith(("http://", "https://")):
            webbrowser.open(target)
        elif target.lower() in APPS:
            subprocess.Popen([APPS[target.lower()]])
        else:
            return "alvo não permitido"
    else:
        return "comando desconhecido"
    return "ok"


async def handler(ws):
    authed = False
    async for raw in ws:
        try:
            msg = json.loads(raw)
        except json.JSONDecodeError:
            continue
        if not authed:
            if msg.get("cmd") == "auth" and TOKEN and hmac.compare_digest(str(msg.get("token", "")), TOKEN):
                authed = True
                await ws.send(json.dumps({"event": "auth_ok"}))
                continue
            await ws.close(code=4001, reason="auth")
            return
        result = run(msg.get("cmd", ""), msg.get("args") or {})
        print("cmd:", msg.get("cmd"), "->", result)


async def main():
    if not TOKEN:
        raise SystemExit("Defina ISABELLE_TOKEN antes de iniciar.")
    async with websockets.serve(handler, HOST, PORT):
        print(f"Isabelle PC agent ouvindo na porta {PORT}")
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
