import importlib.util
import pathlib
import time

spec = importlib.util.spec_from_file_location("agent", pathlib.Path(__file__).parent.parent / "xalane-agent.py")
agent = importlib.util.module_from_spec(spec)
spec.loader.exec_module(agent)

NOW = int(time.time())
DUMP = (
    "privkey\tpubkey\t51820\toff\n"
    f"AAA=\t(none)\t1.2.3.4:5000\t10.8.0.2/32\t{NOW - 10}\t100\t200\t25\n"
    "BBB=\t(none)\t(none)\t10.8.0.3/32\t0\t0\t0\toff\n"
    f"CCC=\t(none)\t5.6.7.8:6000\t10.8.0.4/32\t{NOW - 900}\t5\t7\toff\n"
)


class FakeWg:
    def __init__(self, dump=DUMP):
        self.dump, self.calls = dump, []

    def __call__(self, args):
        self.calls.append(args)
        return self.dump if args[0] == "show" else ""


class FakeApi:
    def __init__(self, desired):
        self.desired, self.beats = desired, []

    def heartbeat(self, active, rx, tx):
        self.beats.append((active, rx, tx))

    def desired_peers(self):
        return self.desired


def test_parses_dump():
    peers, active, rx, tx = agent.current_peers("wg0", FakeWg())
    assert peers == {"AAA=": "10.8.0.2/32", "BBB=": "10.8.0.3/32", "CCC=": "10.8.0.4/32"}
    assert active == 1  # only AAA= had a recent handshake
    assert (rx, tx) == (105, 207)


def test_plan_adds_updates_and_removes():
    upsert, remove = agent.plan(
        {"AAA=": "10.8.0.2/32", "BBB=": "10.8.0.3/32"},
        {"AAA=": "10.8.0.2/32", "BBB=": "10.8.0.9/32", "DDD=": "10.8.0.5/32"},
    )
    assert upsert == {"BBB=": "10.8.0.9/32", "DDD=": "10.8.0.5/32"}
    assert remove == []
    assert agent.plan({"X=": "1"}, {}) == ({}, ["X="])


def test_sync_once_runs_wg_commands():
    wg = FakeWg()
    api = FakeApi({"AAA=": "10.8.0.2/32", "DDD=": "10.8.0.5/32"})
    assert agent.sync_once(api, "wg0", wg) == (1, 2)
    assert api.beats == [(1, 105, 207)]
    assert ["set", "wg0", "peer", "DDD=", "allowed-ips", "10.8.0.5/32"] in wg.calls
    assert ["set", "wg0", "peer", "BBB=", "remove"] in wg.calls
    assert ["set", "wg0", "peer", "CCC=", "remove"] in wg.calls
