class Script:
    def run(self, gateway, ScriptID):
        self.gateway = gateway
        self.ui = gateway.jvm.haven.purus.pbot.PBotAPI.ui()
        self.pbotutils = gateway.jvm.haven.purus.pbot.PBotUtils
        window = self.pbotutils.PBotWindow(self.ui, "Butcher", 110, 110, ScriptID)
        start = window.addButton("skin", "Start", 100, 5, 85)

    def skin(self, gateway, ScriptID):
        self.ui = gateway.jvm.haven.purus.pbot.PBotAPI.ui()
        self.pbotutils = gateway.jvm.haven.purus.pbot.PBotUtils
        self.pbotutils.sysMsg(self.ui, "HI")
