#include <chrono>
#include <cstdio>
#include <thread>
#include <iostream>
#include "ntcore.h"

//list all entries
//std::vector<EntryInfo> GetEntryInfo(StringRef prefix, unsigned int types);
using namespace std;
using namespace nt;

void list()
{
	nt::StringRef  prefix;
	std::vector<nt::EntryInfo> entries=nt::GetEntryInfo( prefix, 0);

	for (auto e : entries)
	{
		cout << "***"<<e.name << std::endl;

	}
	cout << "---------" << endl;

}
/*
std::function<void(unsigned int uid, StringRef name,
	std::shared_ptr<Value> value,
	*///unsigned int flags)

void cb(unsigned int uid, StringRef name,
		std::shared_ptr<Value> value,
		unsigned int flags)
{
	cout << "name:" << name << " value:" << value << endl;
}



void monitor()
{
	list();
	while (1)
		std::this_thread::sleep_for(std::chrono::seconds(2));
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
 
  StringRef pre;

  //EntryListenerCallback

  /** NetworkTables notififier kinds. */
  //enum NT_NotifyKind {
	 // NT_NOTIFY_NONE = 0,
	 // NT_NOTIFY_IMMEDIATE = 0x01, /* initial listener addition */
	 // NT_NOTIFY_LOCAL = 0x02,     /* changed locally */
	 // NT_NOTIFY_NEW = 0x04,       /* newly created entry */
	 // NT_NOTIFY_DELETE = 0x08,    /* deleted */
	 // NT_NOTIFY_UPDATE = 0x10,    /* value changed */
	 // NT_NOTIFY_FLAGS = 0x20      /* flags changed */
  //};

  AddEntryListener(pre, cb, NT_NOTIFY_IMMEDIATE| NT_NOTIFY_NEW| NT_NOTIFY_UPDATE);


  thread t1(monitor);

  t1.join();
  



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
