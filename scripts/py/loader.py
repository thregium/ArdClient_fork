import os
import sys

sys.path.insert(0, os.path.dirname(os.path.realpath(__file__)))
import importlib
import threading
from py4j.java_gateway import JavaGateway, CallbackServerParameters, GatewayParameters
from __pbot.variables import Vars

ScriptVars = {}


# vars storage
def getvar(scriptid, gw):
    if scriptid not in ScriptVars:
        ScriptVars[scriptid] = Vars(scriptid, gw)
    return ScriptVars[scriptid]


class PBotRunner(object):
    def start(scriptname, methodname, scriptid):
        importlib.invalidate_caches()
        script = importlib.import_module(scriptname)
        importlib.reload(script)
        values = getvar(scriptid, gateway)
        threading.Thread(target=getattr(script.Script(), methodname), args=[values]).start()

    class Java:
        implements = ["haven.purus.pbot.Py4j.PBotScriptLoader"]


gateway = JavaGateway(callback_server_parameters=CallbackServerParameters(),
                      python_server_entry_point=PBotRunner,
                      gateway_parameters=GatewayParameters(auto_field=True, auto_convert=True))
