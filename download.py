from parlai.core.dict import DictionaryAgent
from parlai.core.worlds import create_task
from parlai.scripts.build_dict import setup_args
from tqdm import trange
import csv
parser = setup_args()
opts = [
    '--task', 'wikipedia:summary',
    '--datapath', 'data',
    '--datatype', 'train:stream',
]
opt = parser.parse_args(opts, print_args=False)
agent = DictionaryAgent(opt)
world = create_task(opt, agent)
num_examples = world.num_examples()
num_episodes = world.num_episodes()
episodes = []
for _ in trange(num_examples):
    examples = []
    while True:
        world.parley()
        example = world.acts[0]
        examples.append(example)
        if world.episode_done():
            episodes.append(examples)
            break
with open('data/wikipedia_summary.csv', 'w') as f:
    writer = csv.writer(f)
    writer.writerow(["title", "text"])
    for e in episodes:
        assert len(e) == 1
        e = e[0]
        p = e['text'].split('\n')
        title = p[0].strip()
        text = ' '.join(p[1:]).strip()
        writer.writerow([title, text])