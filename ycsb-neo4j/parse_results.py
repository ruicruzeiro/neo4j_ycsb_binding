#!/usr/bin/env python3
"""
parse_results.py — lê os ficheiros de output do YCSB e gera gráficos.

Uso:
    python3 parse_results.py

Gera:
    throughput_workloads.png  — throughput por workload
    latency_workloads.png     — latência p99 por workload
    scalability.png           — throughput vs número de threads
"""

import os
import re
import matplotlib.pyplot as plt

RESULTS_DIR = "results"

# ------------------------------------------------------------------ parser

def parse_ycsb_file(filepath):
    """Extrai métricas de um ficheiro de output YCSB."""
    metrics = {}
    with open(filepath) as f:
        for line in f:
            # Throughput global
            m = re.search(r'\[OVERALL\], Throughput\(ops/sec\), ([\d.]+)', line)
            if m:
                metrics['throughput'] = float(m.group(1))

            # Latência média de READ
            m = re.search(r'\[READ\], AverageLatency\(us\), ([\d.]+)', line)
            if m:
                metrics['read_avg_latency'] = float(m.group(1))

            # Latência p99 de READ
            m = re.search(r'\[READ\], 99thPercentileLatency\(us\), ([\d.]+)', line)
            if m:
                metrics['read_p99_latency'] = float(m.group(1))

            # Latência média de UPDATE
            m = re.search(r'\[UPDATE\], AverageLatency\(us\), ([\d.]+)', line)
            if m:
                metrics['update_avg_latency'] = float(m.group(1))

    return metrics

# ------------------------------------------------------------------ gráficos

def plot_workloads():
    """Throughput e latência para os workloads A–F."""
    workloads = ['a', 'b', 'c', 'd', 'e', 'f']
    throughputs = []
    p99_latencies = []

    for wl in workloads:
        path = os.path.join(RESULTS_DIR, f"workload_{wl}.txt")
        if not os.path.exists(path):
            print(f"  Aviso: {path} não encontrado, a saltar.")
            throughputs.append(0)
            p99_latencies.append(0)
            continue
        m = parse_ycsb_file(path)
        throughputs.append(m.get('throughput', 0))
        p99_latencies.append(m.get('read_p99_latency', 0))

    labels = [f"Workload {w.upper()}" for w in workloads]

    # -- Throughput --
    fig, ax = plt.subplots(figsize=(9, 5))
    bars = ax.bar(labels, throughputs, color='steelblue', edgecolor='white')
    ax.set_title('Throughput por Workload — Neo4j', fontsize=14, fontweight='bold')
    ax.set_ylabel('Throughput (ops/sec)')
    ax.set_xlabel('Workload')
    for bar, val in zip(bars, throughputs):
        if val > 0:
            ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 5,
                    f'{val:.0f}', ha='center', va='bottom', fontsize=9)
    plt.tight_layout()
    out = 'throughput_workloads.png'
    plt.savefig(out, dpi=150)
    print(f"  Guardado: {out}")
    plt.close()

    # -- Latência p99 --
    fig, ax = plt.subplots(figsize=(9, 5))
    bars = ax.bar(labels, p99_latencies, color='tomato', edgecolor='white')
    ax.set_title('Latência p99 (READ) por Workload — Neo4j', fontsize=14, fontweight='bold')
    ax.set_ylabel('Latência p99 (µs)')
    ax.set_xlabel('Workload')
    for bar, val in zip(bars, p99_latencies):
        if val > 0:
            ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 5,
                    f'{val:.0f}', ha='center', va='bottom', fontsize=9)
    plt.tight_layout()
    out = 'latency_workloads.png'
    plt.savefig(out, dpi=150)
    print(f"  Guardado: {out}")
    plt.close()


def plot_scalability():
    """Throughput vs número de threads."""
    threads_list = [1, 5, 10, 20, 50]
    throughputs = []

    for t in threads_list:
        path = os.path.join(RESULTS_DIR, f"scale_threads_{t}.txt")
        if not os.path.exists(path):
            print(f"  Aviso: {path} não encontrado, a saltar.")
            throughputs.append(0)
            continue
        m = parse_ycsb_file(path)
        throughputs.append(m.get('throughput', 0))

    fig, ax = plt.subplots(figsize=(9, 5))
    ax.plot(threads_list, throughputs, marker='o', linewidth=2,
            color='seagreen', markersize=8)
    ax.fill_between(threads_list, throughputs, alpha=0.15, color='seagreen')
    ax.set_title('Escalabilidade — Throughput vs Threads (Workload A)', 
                 fontsize=14, fontweight='bold')
    ax.set_ylabel('Throughput (ops/sec)')
    ax.set_xlabel('Número de Threads')
    ax.set_xticks(threads_list)
    for x, y in zip(threads_list, throughputs):
        if y > 0:
            ax.annotate(f'{y:.0f}', (x, y), textcoords="offset points",
                        xytext=(0, 10), ha='center', fontsize=9)
    plt.tight_layout()
    out = 'scalability.png'
    plt.savefig(out, dpi=150)
    print(f"  Guardado: {out}")
    plt.close()


# ------------------------------------------------------------------ main

if __name__ == '__main__':
    print("A gerar gráficos a partir dos resultados YCSB...")
    plot_workloads()
    plot_scalability()
    print("Concluído!")
