import os
import sys

sys.path.insert(0, os.path.dirname(os.path.realpath(__file__)))
import importlib

from py4j.java_gateway import JavaGateway, CallbackServerParameters, GatewayParameters
from __pbot.thread import Thread

Scripts = {}


class PBotRunner(object):
    def start(scriptname, methodname, scriptid):
        importlib.invalidate_caches()
        script = importlib.import_module(scriptname)
        importlib.reload(script)

        if scriptid not in Scripts:
            Scripts[scriptid] = script.Script(scriptid, gateway)

        tscript = Scripts[scriptid]
        tscript.threads = {}

        if methodname in tscript.threads:
            tscript.threads[methodname].terminate()
        tscript.threads[methodname] = Thread(target=getattr(Scripts[scriptid], methodname), daemon=True)
        tscript.threads[methodname].start()

    def stop(scriptid):
        if scriptid in Scripts:
            for method in Scripts[scriptid].threads:
                Scripts[scriptid].threads[method].terminate()

    class Java:
        implements = ["haven.purus.pbot.Py4j.PBotScriptLoader"]


gateway = JavaGateway(callback_server_parameters=CallbackServerParameters(),
                      python_server_entry_point=PBotRunner,
                      gateway_parameters=GatewayParameters(auto_field=True, auto_convert=True))
