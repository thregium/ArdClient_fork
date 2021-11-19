class Script:
    def run(self, v):
        v.window = v.pbotutils().PBotWindow(v.ui, "Butcher", 110, 110, v.scriptid)
        v.start = v.window.addButton("skin", "Start", 100, 5, 85)

    def skin(self, v):
        v.pbotutils().sysMsg(v.ui, "HI")
