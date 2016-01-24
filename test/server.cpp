#include <chrono>
#include <cstdio>
#include <thread>
#include <iostream>
#include "ntcore.h"

using namespace std;


void monitor()
{
	double d = 0;

	while (1)
	{
		nt::SetEntryValue("/foo2", nt::Value::MakeDouble(d++));
		cout << "changed /foo2 to:" << d << endl;

		std::this_thread::sleep_for(std::chrono::seconds(1));
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
  nt::StartServer("persistent.ini", "", 10000);
  std::this_thread::sleep_for(std::chrono::seconds(1));
  nt::SetEntryValue("/foo", nt::Value::MakeDouble(0.5));
  nt::SetEntryFlags("/foo", NT_PERSISTENT);
  nt::SetEntryValue("/foo2", nt::Value::MakeDouble(0.5));
  //nt::SetEntryValue("/foo2", nt::Value::MakeDouble(0.7));
  //nt::SetEntryValue("/foo2", nt::Value::MakeDouble(0.6));
  //nt::SetEntryValue("/foo2", nt::Value::MakeDouble(0.5));
 // std::this_thread::sleep_for(std::chrono::seconds(10));
  //std::string name;

  //std::cout << "blah";
  

  thread t1(monitor);
  t1.join();

  //std::getline(std::cin, name);
}
