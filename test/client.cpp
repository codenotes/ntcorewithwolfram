#include <chrono>
#include <cstdio>
#include <thread>
#include <iostream>
#include "ntcore.h"

//list all entries
//std::vector<EntryInfo> GetEntryInfo(StringRef prefix, unsigned int types);
using namespace std;

void list()
{
	nt::StringRef  prefix;
	std::vector<nt::EntryInfo> entries=nt::GetEntryInfo( prefix, 0);

	for (auto e : entries)
	{
		cout << "***"<<e.name << std::endl;

	}
}

int main() {
  nt::SetLogger(
      [](unsigned int level, const char* file, unsigned int line,
         const char* msg) {
        std::fputs(msg, stderr);
        std::fputc('\n', stderr);
      },
      0);
  nt::StartClient("127.0.0.1", 10000);
  std::this_thread::sleep_for(std::chrono::seconds(2));

  list();

  return 0;

  auto foo = nt::GetEntryValue("/foo");
  if (foo && foo->IsDouble()) printf("Got foo: %g\n", foo->GetDouble());
  nt::SetEntryValue("/bar", nt::Value::MakeBoolean(false));
  nt::SetEntryFlags("/bar", NT_PERSISTENT);
  nt::SetEntryValue("/bar2", nt::Value::MakeBoolean(true));
  nt::SetEntryValue("/bar2", nt::Value::MakeBoolean(false));
  nt::SetEntryValue("/bar2", nt::Value::MakeBoolean(true));
  std::this_thread::sleep_for(std::chrono::seconds(10));
}
