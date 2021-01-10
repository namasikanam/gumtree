import os, json, subprocess
from tqdm import tqdm
from multiprocessing import Pool
from IPython import embed

HOMEWORK_DIR = '/root/THU2020-2-2-Landmine'
UNKNOWN_PATH = '/root/gumtree/dist/build/install/gumtree/bin/gumtree'

# 找到所有的学生
student_paths = []
for student in os.listdir(HOMEWORK_DIR):
    student_dir = os.path.join(HOMEWORK_DIR, student)
    cpps = list(filter(lambda filename: filename.endswith('.cpp'), os.listdir(student_dir)))
    if len(cpps) == 1:
        student_paths.append(os.path.join(student_dir, cpps[0]))

def worker(student1):
    json_path = os.path.join(os.path.dirname(student1), 'result.json')
    if os.path.isfile(json_path):
        with open(json_path, 'r') as f:
            similarities = json.load(f)
    else:
        similarities = {}
        for i, student2 in enumerate(student_paths):
            if student1 != student2:
                # print('calculating similarity between {} and {}...'.format(student1, student2))

                cp = subprocess.run([UNKNOWN_PATH, student1, student2], stdout = subprocess.PIPE)
                similarities[student2] = float(cp.stdout)

                print('similarity between {} and {} is {} ({})'.format(student1.split('/')[-2], student2.split('/')[-2], similarities[student2], i))

        # 把得到的 Jaccard Similarity 存下来，保证一定的 robustness
        with open(json_path, 'w') as f:
            json.dump(similarities, f)

pool = Pool(104)
pool.map_async(worker, student_paths)
pool.close()
pool.join()

pair_similarities = []
for student1 in student_paths:
    json_path = os.path.join(os.path.dirname(student1), 'result.json')
    if os.path.isfile(json_path):
        with open(json_path, 'r') as f:
            similarities = json.load(f)
    for student2 in similarities:
        pair_similarities.append((student1, student2, similarities[student2]))

# 排序，并输出
pair_similarities.sort(key = lambda t: t[2], reverse = True)
with open('results.json', 'w') as f:
    json.dump(pair_similarities, f, indent = '\t')